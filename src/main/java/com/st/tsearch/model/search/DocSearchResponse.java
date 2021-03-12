package com.st.tsearch.model.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocSearchResponse {

    /* total result number */
    private int total;

    /* result docs */
    private List<String> docIds;

    /**
     * Build the response for doc list
     * @param docList result doc list
     * @return response object
     */
    public static DocSearchResponse of(List<String> docList) {
        List<String> docIds = Optional.ofNullable(docList).orElse(new ArrayList<>());
        return new DocSearchResponse(docIds.size(), docIds);
    }
}
