package com.st.tsearch.model.search;

import com.st.tsearch.model.rustful.BaseParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Query param for doc search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordListSearchParam extends BaseParam {

    private List<String> keywordList = new ArrayList<>();
}
