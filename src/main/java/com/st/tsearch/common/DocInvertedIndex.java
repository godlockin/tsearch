package com.st.tsearch.common;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DocInvertedIndex {
    private static ConcurrentHashMap<String, Set<String>> index = new ConcurrentHashMap<>();

    public static void indexToken(String token, String docId) {
        index.putIfAbsent(token, ConcurrentHashMap.newKeySet());
        index.get(token).add(docId);
    }

    public static Set<String> retrieveDoc(String token) {
        return index.getOrDefault(token, ConcurrentHashMap.newKeySet());
    }
}
