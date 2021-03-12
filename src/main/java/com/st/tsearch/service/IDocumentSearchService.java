package com.st.tsearch.service;

import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.search.DocRetrieveParam;
import com.st.tsearch.model.search.DocSearchParam;
import com.st.tsearch.model.search.DocSearchResponse;

import java.util.List;

/**
 * Interface for document search services
 */
public interface IDocumentSearchService {

    /**
     * Searching docs against the keyword
     *
     * @param param doc search param
     * @return docId list against the param
     */
    DocSearchResponse keywordSearch(DocSearchParam param);

    /**
     * Searching docs against the query, the differences from keyword search is:
     *  system will try to analyze the query and find out all the target keywords
     *
     * @param param doc search param
     * @return docId list against the param
     */
    DocSearchResponse search(DocSearchParam param);

    /**
     * Retrieve the doc against the docId
     *
     * @param docId target docId
     * @return doc info for id
     */
    DocUnit retrieveSingleDoc(String docId);

    /**
     * Retrieve the docs against the id list
     *
     * @param param doc retrieve param
     * @return doc info for id list
     */
    List<DocUnit> retrieveMultiDocs(DocRetrieveParam param);
}
