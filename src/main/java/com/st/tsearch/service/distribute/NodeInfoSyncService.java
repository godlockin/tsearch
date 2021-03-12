package com.st.tsearch.service.distribute;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.st.tsearch.common.NodeState;
import com.st.tsearch.common.constants.ResultEnum;
import com.st.tsearch.common.utils.RestHttpClient;
import com.st.tsearch.exception.TSearchException;
import com.st.tsearch.model.cluster.NodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NodeInfoSyncService {

    @Value("${MAX_PING_RETRY:3}")
    private int MAX_PING_RETRY;

    @Value("${MAX_PING_GAP:1000000}")
    private long MAX_PING_GAP;

    /**
     * Sync the node's info to whole cluster
     */
    public void nodesAck() {
        NodeInfo nodeInfo = NodeState.getAsObject("NODE", "NODE_INFO", new NodeInfo(), NodeInfo.class);
        if (ObjectUtils.isEmpty(nodeInfo)) {
            throw new TSearchException(ResultEnum.SYSTEM_NODE_STATE);
        }

        JSONObject json = JSON.parseObject(JSON.toJSONString(nodeInfo));
        ConcurrentHashMap<String, Object> nodeMap =
                NodeState.getAsConcurrentHashMap("CLUSTER", "NODE_MAP", new ConcurrentHashMap<>());
        ConcurrentHashMap.KeySetView<String, Object> keySetView = nodeMap.keySet();
        keySetView.parallelStream().filter(node -> !"_local_".equalsIgnoreCase(node))
                .forEach(node -> nodeAckHandler(node, json, nodeMap));
    }

    /**
     * Handle the node ack jobs
     *
     * @param node remote node url
     * @param json current node status
     * @param nodeMap cached all nodes' map
     */
    private void nodeAckHandler(String node, Map<String, Object> json, ConcurrentHashMap<String, Object> nodeMap) {
        NodeInfo nodeInfo = (NodeInfo) nodeMap.get(node);
        String nodeId = nodeInfo.getNodeId();
        long now = System.currentTimeMillis();

        String response = RestHttpClient.doPost(node, json);
        if (StringUtils.isBlank(response)) {
            nodeInfo.setStatus("RED");
            log.warn("Node [{}]-[{}] no response", nodeId, node);

            int retry = nodeInfo.getPingRetry();
            long lastPing = nodeInfo.getLastPing();
            long gap = now - lastPing;
            if (0 > lastPing || MAX_PING_GAP < gap) {
                log.warn("Node [{}] maybe lost, after {} from last ping", nodeId, gap);
                if (MAX_PING_RETRY < retry) {
                    log.error("We will drop it as we retried {} times over max retry gap {}", retry, MAX_PING_RETRY);
                    nodeInfo.setStatus("LOST");
                }
                nodeInfo.setPingRetry(retry + 1);
            }
            return;
        }

        NodeInfo remoteNodeInfo = JSON.parseObject(response, NodeInfo.class);
        remoteNodeInfo.setLastPing(now);
        remoteNodeInfo.setPingRetry(0);
        nodeMap.put(node, remoteNodeInfo);
        log.info("Node status: [{}]", remoteNodeInfo);
    }

    /**
     * Receive & handle the remote ping request
     *
     * @param remoteNodeInfo remote ping
     */
    public void remotePing(NodeInfo remoteNodeInfo) {
        String url = remoteNodeInfo.getUrl();
        ConcurrentHashMap<String, Object> nodeMap = NodeState.getAsConcurrentHashMap("CLUSTER"
                , "NODE_MAP", new ConcurrentHashMap<>());
        NodeInfo nodeInfo = (NodeInfo) nodeMap.getOrDefault(url, remoteNodeInfo);
        BeanUtils.copyProperties(remoteNodeInfo, nodeInfo);
        nodeInfo.setPingRetry(0);
        nodeInfo.setLastPing(System.currentTimeMillis());
        nodeInfo.setStatus("GREEN");
        nodeMap.put(url, nodeInfo);
    }
}
