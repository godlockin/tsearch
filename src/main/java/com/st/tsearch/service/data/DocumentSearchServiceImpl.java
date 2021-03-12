package com.st.tsearch.service.data;

import com.st.tsearch.common.Dictionary;
import com.st.tsearch.common.DocCache;
import com.st.tsearch.common.DocInvertedIndex;
import com.st.tsearch.common.utils.DataUtils;
import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.search.DocRetrieveParam;
import com.st.tsearch.model.search.DocSearchParam;
import com.st.tsearch.model.search.DocSearchResponse;
import com.st.tsearch.service.IDocumentSearchService;
import com.st.tsearch.service.analysis.DataAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Implement for the document searching interface
 */
@Slf4j
@Service
public class DocumentSearchServiceImpl implements IDocumentSearchService {

    @Override
    public DocSearchResponse keywordSearch(DocSearchParam param) {

        List<String> docList = new ArrayList<>();
        String query = Optional.ofNullable(param.getQuery()).orElse("").trim();
        if (!(paramValidate(param) || Dictionary.isContains(query))) {
            log.warn("Param is illegal");
            return DocSearchResponse.of(docList);
        }

        docList.addAll(DocInvertedIndex.retrieveDoc(query));
        List<String> targetList = DataUtils.handlePaging(param.getPage(), param.getSize(), docList);
        log.info("Found {} docs contains keyword:[{}] in total, returns {} docs at least"
                , docList.size(), query, targetList.size());
        return DocSearchResponse.of(targetList);
    }

    @Override
    public DocSearchResponse search(DocSearchParam param) {

        List<String> docList = Collections.synchronizedList(new ArrayList<>());
        if (!paramValidate(param)) {
            log.warn("Param is illegal");
            return DocSearchResponse.of(docList);
        }

        String query = param.getQuery().trim();
        List<String> tokens = DataAnalyzer.analyzeContent(query);
        if (CollectionUtils.isEmpty(tokens)) {
            log.warn("No available keyword found for query:[{}]", query);
            return DocSearchResponse.of(docList);
        }

        tokens.parallelStream()
                .map(DocInvertedIndex::retrieveDoc)
                .flatMap(Collection::parallelStream)
                .distinct()
                .sorted()
                .forEach(docList::add);

        List<String> targetList = DataUtils.handlePaging(param.getPage(), param.getSize(), docList);
        log.info("Found {} docs contains keyword:[{}] in total, returns {} docs at least"
                , docList.size(), query, targetList.size());
        return DocSearchResponse.of(targetList);
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
    private boolean paramValidate(DocSearchParam param) {
        String query = param.getQuery();
        if (StringUtils.isBlank(query)) {
            log.warn("No query found");
            return false;
        }

        if (0 >= param.getSize()) {
            log.warn("Empty page wanted");
            return false;
        }

        return true;
    }
}
