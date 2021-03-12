package com.st.tsearch.model.search;

import com.st.tsearch.model.rustful.BaseParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query param for doc search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordSearchParam extends BaseParam {

    private String query;
}
