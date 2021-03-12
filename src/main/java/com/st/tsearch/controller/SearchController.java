package com.st.tsearch.controller;

import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.search.DocRetrieveParam;
import com.st.tsearch.model.search.DocSearchParam;
import com.st.tsearch.model.search.DocSearchResponse;
import com.st.tsearch.service.IDocumentSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Restful api for searching docs
 */
@Slf4j
@RestController
public class SearchController {

    @Autowired
    private IDocumentSearchService documentSearchService;

    /**
     * Search function for keyword search
     *
     * @param param search params
     * @return target docIds
     */
    @RequestMapping(value = { "/_keywordSearch" }, method = RequestMethod.POST)
    public ResponseEntity<DocSearchResponse> keywordSearch(@RequestBody DocSearchParam param) {
        return ResponseEntity.ok(documentSearchService.keywordSearch(param));
    }

    /**
     * Search function for normal search
     *
     * @param param search params
     * @return target docIds
     */
    @RequestMapping(value = { "/_search" }, method = RequestMethod.POST)
    public ResponseEntity<DocSearchResponse> normalSearch(@RequestBody DocSearchParam param) {
        return ResponseEntity.ok(documentSearchService.search(param));
    }

    /**
     * Retrieve single doc info
     *
     * @param docId target docId
     * @return target doc info
     */
    @RequestMapping(value = { "/_getDoc/{docId}" }, method = RequestMethod.GET)
    public ResponseEntity<DocUnit> retrieveSingleDoc(@PathVariable("docId") String docId) {
        return ResponseEntity.ok(documentSearchService.retrieveSingleDoc(docId));
    }

    /**
     * Retrieve multi doc info
     *
     * @param param target id list
     * @return target docs' info
     */
    @RequestMapping(value = { "/_mgetDocs" }, method = RequestMethod.POST)
    public ResponseEntity<List<DocUnit>> retrieveMultiDocs(@RequestBody DocRetrieveParam param) {
        return ResponseEntity.ok(documentSearchService.retrieveMultiDocs(param));
    }
}
