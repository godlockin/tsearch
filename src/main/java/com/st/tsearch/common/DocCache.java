package com.st.tsearch.common;

import com.st.tsearch.model.doc.DocUnit;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class DocCache {
    private static ConcurrentHashMap<String, DocUnit> cache = new ConcurrentHashMap<>();

    public static boolean addDoc(DocUnit docUnit) {
        String docId = docUnit.getDocId();
        if (!cache.containsKey(docId)) {
            cache.put(docId, docUnit);
            return true;
        }

        DocUnit old = cache.get(docId);
        if (old.getVersion() <= docUnit.getVersion()) {
            cache.put(docId, docUnit);
            return true;
        }
        return false;
    }

    public static DocUnit getDoc(String docId) {
        return cache.get(docId);
    }
}
