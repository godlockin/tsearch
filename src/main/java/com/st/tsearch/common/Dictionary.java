package com.st.tsearch.common;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Dictionary {
    private static volatile int minToken = Integer.MAX_VALUE;

    private static AtomicInteger tokenCount = new AtomicInteger(0);
    private static Set<Integer> tokenSize = ConcurrentHashMap.newKeySet();
    private static Set<String> tokenSet = ConcurrentHashMap.newKeySet();

    public static int getMinToken() {
        return minToken;
    }

    public static Set<Integer> getTokenSize() {
        return tokenSize;
    }

    public static boolean isContains(String token) {
        return tokenSet.contains(token);
    }

    public static boolean addToken(String token) {
        boolean addTokenSuccess = tokenSet.add(token);
        if (addTokenSuccess) {
            tokenSize.add(token.length());
            minToken = Math.min(token.length(), minToken);
            tokenCount.incrementAndGet();
        }
        return addTokenSuccess;
    }

    public static Integer getTokenCount() {
        return tokenCount.intValue();
    }

    public static List<String> loadAll() {
        return new ArrayList<>(tokenSet);
    }
}
