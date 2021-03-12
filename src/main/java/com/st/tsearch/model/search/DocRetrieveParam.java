package com.st.tsearch.model.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Query param for doc retrieve
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocRetrieveParam {

    private List<String> docIds;
}
