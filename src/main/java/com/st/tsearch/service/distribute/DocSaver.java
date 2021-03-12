package com.st.tsearch.service.distribute;

import com.alibaba.fastjson.JSON;
import com.st.tsearch.common.Dictionary;
import com.st.tsearch.common.DocCache;
import com.st.tsearch.common.DocInvertedIndex;
import com.st.tsearch.common.NodeState;
import com.st.tsearch.common.utils.DataUtils;
import com.st.tsearch.common.utils.RestHttpClient;
import com.st.tsearch.model.cluster.NodeInfo;
import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.save.DocSaveResponse;
import com.st.tsearch.service.analysis.DataAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DocSaver {

    @Value("${DOC_LOAD_FORK_SIZE:10}")
    private Integer FORK_SIZE;
    private static final BlockingQueue<DocUnit> queue = new LinkedBlockingQueue<>();
    public static boolean push(DocUnit item) {
        return queue.offer(item);
    }

    @PostConstruct
    void init() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this::docIndexProcess
                        , 1000
                        , 10_000
                        , TimeUnit.MILLISECONDS);
    }

    /**
     * Doc indexing process
     */
    private void docIndexProcess() {
        if (0 >= Dictionary.getTokenCount()) {
            log.warn("Dictionary is not ready");
            return;
        }

        List<DocUnit> list = new ArrayList<>();
        queue.drainTo(list);
        if (CollectionUtils.isEmpty(list)) {
            log.debug("No doc found for now");
            return;
        }

        // cache the doc -> tokens info, to skip the tokens which has pointing to doc
        ForkJoinPool forkJoinPool = new ForkJoinPool(FORK_SIZE);
        forkJoinPool.submit(() -> docIndexer(list));
        log.info("Indexed {} documents", list.size());
    }

    /**
     * Analyze the doc and index the tokens
     *
     * @param list doc list
     */
    public ConcurrentHashMap<String, DocSaveResponse> docIndexer(List<DocUnit> list) {
        ConcurrentHashMap<String, DocSaveResponse> resultMap = new ConcurrentHashMap<>();
        if (CollectionUtils.isEmpty(list)) {
            log.warn("No doc found");
            return resultMap;
        }

        ConcurrentHashMap<String, Object> nodeMap = NodeState.getAsConcurrentHashMap("CLUSTER", "NODE_MAP", new ConcurrentHashMap<>());
        int nodeNum = nodeMap.size();

        // node finder
        Function<DocUnit, Integer> groupClassifier = unit -> unit.getDocId().hashCode() % nodeNum;

        // init the doc counter
        AtomicInteger docCounter = new AtomicInteger(0);

        // loop all the docs and save the index against token VS. doc
        ConcurrentMap<Integer, List<DocUnit>> groupingDoc = list.parallelStream()
                .peek(unit -> unit.setDocId(StringUtils.isBlank(unit.getDocId()) ? DataUtils.initId() : unit.getDocId()))
                .collect(Collectors.groupingByConcurrent(groupClassifier));

        // index docs
        List<MutablePair<String, NodeInfo>> nodeList = nodeMap.entrySet().stream()
                .map(e -> MutablePair.of(e.getKey(), (NodeInfo) e.getValue()))
                .sorted(Comparator.comparingInt(p -> p.getValue().getNodeId().hashCode()))
                .collect(Collectors.toList());

        DataUtils.forEach(nodeNum, nodeList, (idx, pair) -> {
            List<DocUnit> docUnits = groupingDoc.getOrDefault(idx, new ArrayList<>());
            docCounter.addAndGet(docUnits.size());

            String nodeId = pair.getValue().getNodeId();
            if (CollectionUtils.isEmpty(docUnits)) {
                log.warn("No doc found for node:[{}]", nodeId);
                resultMap.put(nodeId, DocSaveResponse.builder().nodeId(nodeId).build());
                return;
            }

            // to index the docs by current node or remote node
            if ("_local_".equalsIgnoreCase(pair.getKey())) {
                NodeInfo nodeInfo = pair.getValue();
                resultMap.put(nodeId, localDocIndexer(docUnits, nodeInfo));
            } else {
                resultMap.put(nodeId, remoteDocIndexer(docUnits, pair));
            }
        });
        log.info("Handle {} docs by {} nodes", docCounter.intValue(), resultMap.size());
        return resultMap;
    }

    /**
     * Save doc list into current node
     *
     * @param list doc list
     * @param nodeInfo current node info
     * @return docs save result
     */
    private DocSaveResponse localDocIndexer(List<DocUnit> list, NodeInfo nodeInfo) {
        List<String> ssList = Collections.synchronizedList(new ArrayList<>());
        List<String> faList = Collections.synchronizedList(new ArrayList<>());

        list.parallelStream().forEach(unit -> {
            String docId = unit.getDocId();
            boolean isSave = DocCache.addDoc(unit);
            if (isSave) {
                List<String> tokens = DataAnalyzer.analyzeContent(unit.getContent());
                tokens.forEach(token -> DocInvertedIndex.indexToken(token, docId));
                ssList.add(docId);
            } else {
                faList.add(docId);
            }
        });

        return DocSaveResponse.builder()
                .nodeId(nodeInfo.getNodeId())
                .success(ssList)
                .failure(faList)
                .build();
    }

    /**
     * Save doc list to remote node by calling its api
     *
     * @param list doc list
     * @param nodeInfo remote node info
     * @return
     */
    private DocSaveResponse remoteDocIndexer(List<DocUnit> list, MutablePair<String, NodeInfo> nodeInfo) {
        NodeInfo node = nodeInfo.getValue();
        List<String> ids = list.stream().map(DocUnit::getDocId).collect(Collectors.toList());
        DocSaveResponse response = DocSaveResponse.builder()
                .nodeId(node.getNodeId())
                .failure(ids)
                .build();
        if ("RED".equalsIgnoreCase(node.getStatus())) {
            return response;
        }

        String url = nodeInfo.getKey() + "/_mDocs";
        Map<String, Object> param = new HashMap<>();
        param.put("data", list);
        String remoteResponse = RestHttpClient.doPost(url, param);
        if (StringUtils.isBlank(remoteResponse)) {
            log.warn("No response from node:[{}]", node.getNodeId());
            // no response mean the remote node maybe failure
            node.setStatus("RED");
            return response;
        }

        return JSON.parseObject(remoteResponse, DocSaveResponse.class);
    }

}
