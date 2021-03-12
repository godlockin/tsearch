package com.st.tsearch.service.analysis;

import com.st.tsearch.common.Dictionary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DataAnalyzer {

    /**
     * Analyze the content and find out all the tokens belongs to dictionary with doc cache
     *  use doc cache for enhance the performance of indexing process
     *
     * @param content target content
     * @return all the tokens
     */
    public static List<String> analyzeContent(String content) {
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isBlank(content)) {
            return tokens;
        }

        content = org.springframework.util.StringUtils.trimAllWhitespace(content);
        int contentLength = content.length();

        // skip the lines which is shorter then the shortest token
        if (contentLength < Dictionary.getMinToken()) {
            return tokens;
        }

        // use token sizes as seed of ngram to analyze the sentences
        // skip the tokens which size isn't equals the ones in dictionary
        Set<Integer> tokenSizes = Dictionary.getTokenSize();
        for (Integer tokenSize : tokenSizes) {
            int start = 0, end;
            while ((end = ++start + tokenSize - 1) < contentLength) {
                String tmp = content.substring(start - 1, end);
                if (Dictionary.isContains(tmp)) {
                    tokens.add(tmp);
                }
            }
        }
        return tokens;
    }
}
