package com.st.tsearch.service.data;

import com.alibaba.fastjson.JSON;
import com.st.tsearch.common.Dictionary;
import com.st.tsearch.common.DocCache;
import com.st.tsearch.common.DocInvertedIndex;
import com.st.tsearch.common.NodeState;
import com.st.tsearch.common.utils.RestHttpClient;
import com.st.tsearch.model.cluster.NodeInfo;
import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.search.DocRetrieveParam;
import com.st.tsearch.model.search.DocSearchResponse;
import com.st.tsearch.model.search.KeywordListSearchParam;
import com.st.tsearch.model.search.KeywordSearchParam;
import com.st.tsearch.service.IDocumentSearchService;
import com.st.tsearch.service.analysis.DataAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implement for the document searching interface
 */
@Slf4j
@Service
public class DocumentSearchServiceImpl implements IDocumentSearchService {

    @Override
    public DocSearchResponse keywordSearch(KeywordSearchParam param) {

        List<String> docList = new ArrayList<>();
        String keyword = Optional.ofNullable(param.getQuery()).orElse("").trim();
        if (!(paramValidate(param) || Dictionary.isContains(keyword))) {
            log.warn("Param is illegal");
            return DocSearchResponse.of(docList);
        }

        docList.addAll(DocInvertedIndex.retrieveDoc(keyword));
        docList.removeIf(StringUtils::isBlank);
        log.info("Found {} docs contains keyword:[{}] in total", docList.size(), keyword);
        return DocSearchResponse.of(docList);
    }

    @Override
    public DocSearchResponse keywordListSearch(KeywordListSearchParam param) {

        Set<String> docSet = ConcurrentHashMap.newKeySet();
        List<String> keywordList = param.getKeywordList();
        if (!CollectionUtils.isEmpty(keywordList)) {
            log.warn("No keyword found");
            return DocSearchResponse.of(new ArrayList<>());
        }

        Set<String> distinct = ConcurrentHashMap.newKeySet();
        keywordList.parallelStream().filter(StringUtils::isNotBlank)
                .map(String::trim)
                .peek(keyword -> {
                    if (Dictionary.isContains(keyword)) {
                        distinct.add(keyword);
                    }
                })
                .map(DocInvertedIndex::retrieveDoc)
                .filter(set -> !CollectionUtils.isEmpty(set))
                .forEach(docSet::addAll);
        log.info("Found {} docs contains keywords:[{}] in total", docSet.size(), distinct);
        return DocSearchResponse.of(new ArrayList<>(docSet));
    }

    @Override
    public DocSearchResponse search(KeywordSearchParam param) {

        Set<String> docSet = ConcurrentHashMap.newKeySet();
        if (!paramValidate(param)) {
            log.warn("Param is illegal");
            return DocSearchResponse.of(new ArrayList<>());
        }

        String query = param.getQuery().trim();
        List<String> tokens = DataAnalyzer.analyzeContent(query);
        if (CollectionUtils.isEmpty(tokens)) {
            log.warn("No available keyword found for query:[{}]", query);
            return DocSearchResponse.of(new ArrayList<>());
        }

        KeywordListSearchParam remoteParam = new KeywordListSearchParam(tokens);
        ConcurrentHashMap<String, Object> nodeMap =
                NodeState.getAsConcurrentHashMap("CLUSTER", "NODE_MAP", new ConcurrentHashMap<>());
        nodeMap.entrySet().parallelStream().forEach(entry -> {
            String url = entry.getKey();
            NodeInfo nodeInfo = (NodeInfo) entry.getValue();
            if ("_local_".equalsIgnoreCase(url)) {
                DocSearchResponse localResponse = keywordListSearch(remoteParam);
                docSet.addAll(localResponse.getDocIds());
            } else {
                docSet.addAll(remoteDocFinder(remoteParam, url, nodeInfo));
            }
        });

        log.info("Built {} docs in total for keywords:[{}] from query:[{}]", docSet.size(), tokens, query);
        return DocSearchResponse.of(new ArrayList<>(docSet));
    }

    /**
     * Call remote node for docs against the keyword list
     *
     * @param remoteParam query param
     * @param url url of remote node
     * @param node node info cached in local node
     * @return doc ids
     */
    private Set<String> remoteDocFinder(KeywordListSearchParam remoteParam, String url, NodeInfo node) {
        if (!"GREEN".equalsIgnoreCase(node.getStatus())) {
            return new HashSet<>();
        }

        String funcUrl = url + "/_keywordListSearch";
        Map<String, Object> param = new HashMap<>();
        param.put("keywordList", remoteParam.getKeywordList());
        String remoteResponse = RestHttpClient.doPost(funcUrl, param);
        if (StringUtils.isBlank(remoteResponse)) {
            log.warn("No response from node:[{}]", node.getNodeId());
            // no response mean the remote node maybe failure
            node.setStatus("RED");
            return new HashSet<>();
        }

        DocSearchResponse response = JSON.parseObject(remoteResponse, DocSearchResponse.class);
        Set<String> resultDocs = CollectionUtils.isEmpty(response.getDocIds()) ?
                new HashSet<>() : new HashSet<>(response.getDocIds());
        log.info("Got {} docs from node:[{}]", resultDocs.size(), node.getNodeId());
        return resultDocs;
    }

    @Override
    public DocUnit retrieveSingleDoc(String docId) {
        DocUnit docUnit = new DocUnit();
        if (StringUtils.isBlank(docId)) {
            log.warn("No docId found");
            return docUnit;
        }

        docId = docId.trim();
        docUnit = DocCache.getDoc(docId);
        log.info("Found doc info for id: [{}] as: {}", docId, docUnit);
        return docUnit;
    }

    @Override
    public List<DocUnit> retrieveMultiDocs(DocRetrieveParam param) {
        List<DocUnit> docUnits = Collections.synchronizedList(new ArrayList<>());
        List<String> idList = param.getDocIds();
        if (CollectionUtils.isEmpty(idList)) {
            log.warn("No docId found");
            return docUnits;
        }

        idList.parallelStream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(DocCache::getDoc)
                .forEach(docUnits::add);
        log.info("Loaded {} doc info against {} doc id", docUnits.size(), idList.size());
        return docUnits;
    }

    /**
     * Validate the input param
     * @param param data search param
     * @return whether the param is legally
     */
    private boolean paramValidate(KeywordSearchParam param) {
        String keyword = param.getQuery();
        if (StringUtils.isBlank(keyword)) {
            log.warn("No keyword found");
            return false;
        }

        if (0 >= param.getSize()) {
            log.warn("Empty page wanted");
            return false;
        }

        return true;
    }
}
