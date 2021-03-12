package com.st.tsearch.service.distribute;

import com.st.tsearch.common.NodeConfig;
import com.st.tsearch.common.NodeState;
import com.st.tsearch.common.constants.Constants.SysConfig;
import com.st.tsearch.common.constants.ResultEnum;
import com.st.tsearch.common.utils.FileUtils;
import com.st.tsearch.common.utils.HostUtils;
import com.st.tsearch.exception.TSearchException;
import com.st.tsearch.model.cluster.NodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
@Repository("clusterManager")
@Component
public class ClusterManager {

    @Value("${NODE_ID:}")
    private String NODE_ID;

    @Value("${CONFIG_PATH:./data/config/clusterInfo.txt}")
    private String CONFIG_PATH;

    @Autowired
    private NodeInfoSyncService nodeInfoSyncService;

    @PostConstruct
    void init() {
        localConfigInit();

        Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(
                        nodeInfoSyncService::nodesAck
                        , 1_000
                        , 10_000
                        , TimeUnit.MILLISECONDS
                );
    }

    /**
     * Whole init process for nodes and other config
     */
    private void localConfigInit() {
        Executor executor = Executors.newFixedThreadPool(5);
        CompletableFuture.supplyAsync(this::loadLocalConfig, executor)
                .thenApplyAsync(this::initLocalNode, executor)
                .thenApplyAsync(this::initRemoteNode, executor)
                .thenApplyAsync(this::initOtherLocalConfig, executor)
                .whenCompleteAsync((__, e) -> {
                    NodeInfo nodeInfo =
                            NodeState.getAsObject("NODE", "NODE_INFO", new NodeInfo(), NodeInfo.class);
                    boolean initSs = ObjectUtils.isEmpty(e);
                    if (initSs) {
                        nodeInfo.setUpTime(System.currentTimeMillis());
                    } else {
                        log.error("Error happened: {}", e);
                    }
                    nodeInfo.setStatus(initSs ? "GREEN" : "RED");
                }, executor).join();
    }

    /**
     * Load the local config file deploy with node
     * @return config info as map
     */
    private Map<String, String> loadLocalConfig() {
        List<String> configLines = new ArrayList<>();
        if (FileUtils.fileExists(CONFIG_PATH)) {
            configLines = FileUtils.loadFileContent(CONFIG_PATH);
        }

        Map<String, String> localConfig = new LinkedHashMap<>();
        configLines.stream().filter(StringUtils::isNotBlank)
                .filter(str -> str.contains(SysConfig.KEY_DELIMITER))
                .filter(str -> !str.startsWith(SysConfig.KEY_POSITION))
                .map(str -> str.replaceAll("[\t\r\n\\s]*", ""))
                .forEachOrdered(str -> {
                    int idx = str.indexOf(SysConfig.KEY_DELIMITER);
                    String key = str.substring(0, idx);
                    String value = str.substring(idx + 1);
                    localConfig.put(key, value);
                });

        if (CollectionUtils.isEmpty(localConfig)) {
            log.warn("No local config loaded");
        }

        NodeConfig.set("NODE#LOCAL_CONFIG", localConfig);
        return localConfig;
    }

    /**
     * Init current node's info
     * @param config local config
     * @return local config
     */
    private Map<String, String> initLocalNode(Map<String, String> config) {

        // init basic info for node itself
        String ip = HostUtils.getIp();
        String hostname = HostUtils.getHostName();
        Integer port = HostUtils.getPort();

        // if nodeId isn't exists
        if (StringUtils.isBlank(NODE_ID)) {

            // try to load from config file
            NODE_ID = config.getOrDefault("NODE#NODE_ID", "");

            // if config file can't load node id, calc a new id
            if (StringUtils.isBlank(NODE_ID)) {
                NODE_ID = String.join("_", hostname, ip, port.toString());
            }
        }

        if (StringUtils.isBlank(NODE_ID)) {
            throw new TSearchException(ResultEnum.SYSTEM_NODE_INIT);
        }

        // cache node itself
        NodeInfo nodeInfo = NodeInfo.builder()
                .ip(ip)
                .port(port)
                .nodeId(NODE_ID)
                .hostname(hostname)
                .url(String.format("http://%s:%s", ip, port))
                .build();
        NodeState.set("NODE", "NODE_INFO", nodeInfo);

        NodeState.set("CLUSTER", "NODE_MAP", new ConcurrentHashMap<>());
        NodeState.getAsConcurrentHashMap("CLUSTER", "NODE_MAP", new ConcurrentHashMap<>())
                .put("_local_", nodeInfo);

        log.info("Node:[{}] init", NODE_ID);
        return config;
    }

    /**
     * Init the node list stored in node local config
     * @param config node local config
     * @return config itself
     */
    private Map<String, String> initRemoteNode(Map<String, String> config) {
        // load the nodes' info
        String nodeStr = config.getOrDefault("CLUSTER#NODE_LIST", "");
        if (StringUtils.isBlank(nodeStr)) {
            return config;
        }

        // cache the nodes' local cache
        ConcurrentHashMap<String, Object> nodeMap =
                NodeState.getAsConcurrentHashMap("CLUSTER", "NODE_MAP", new ConcurrentHashMap<>());
        Set<String> nodeList = NodeState.getAsSet("CLUSTER", "NODE_LIST", ConcurrentHashMap.newKeySet());
        Stream.of(nodeStr.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .peek(nodeList::add)
                .forEach(str -> nodeMap.putIfAbsent(str, new NodeInfo(str)));
        return config;
    }

    /**
     * Init other node config from config file
     * @param config config info
     * @return config info
     */
    private Map<String, String> initOtherLocalConfig(Map<String, String> config) {

        String DICT_PATH = config.getOrDefault("NODE#DICT_PATH", "");
        if (StringUtils.isNotBlank(DICT_PATH)) {
            NodeConfig.set("NODE#DICT_PATH", DICT_PATH);
        }

        return config;
    }
}
