package com.st.tsearch.common;

import com.alibaba.fastjson.JSON;
import com.st.tsearch.common.utils.FileUtils;
import com.st.tsearch.common.utils.XXHash;
import com.st.tsearch.model.cluster.NodeInfo;
import com.st.tsearch.model.doc.DocUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class DocCache {

    @Value("${CACHE_FILE_PATH:./data/cache/%s.txt}")
    private String CACHE_FILE_PATH_PATTERN;
    private static String CACHE_FILE_PATH;

    private static int CACHE_SEED = Integer.MAX_VALUE;
    private static int CACHE_GAP = 600_000;
    private static ConcurrentHashMap<String, DocUnit> cache = new ConcurrentHashMap<>();
    private static int[] bitmap = new int[CACHE_SEED];

    public static boolean addDoc(DocUnit docUnit) {
        String docId = docUnit.getDocId();
        DocUnit old = cache.getOrDefault(docId, new DocUnit());
        if (old.getVersion() > docUnit.getVersion()) {
            return false;
        }

        fulfillDocInfo(docUnit);
        cache.put(docId, docUnit);
        return true;
    }

    public static DocUnit getDoc(String docId) {
        DocUnit doc = cache.getOrDefault(docId, new DocUnit());
        if (StringUtils.isBlank(doc.getDocId()) || !doc.isLoad()) {
            return doc;
        }

        int idx = findBitPosition(docId);
        List<String> lines = FileUtils.loadFileContent(CACHE_FILE_PATH, idx - 1);
        if (CollectionUtils.isEmpty(lines)) {
            return doc;
        }

        String content = Arrays.stream(lines.get(0).split(":"))
                .skip(2).collect(Collectors.joining(":"));
        doc.setContent(content);
        return doc;
    }

    @PostConstruct
    void init() {
        final ThreadFactory threadFactory = new ThreadFactory() {
            final AtomicLong atomicLong = new AtomicLong(0L);

            @Override
            public Thread newThread(Runnable r) {

                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName(getClass() + "_" + r.getClass() + "_" + atomicLong.incrementAndGet());
                thread.setDaemon(false);
                return thread;
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(3, threadFactory);
        executorService.submit(new CacheFlush());
        executorService.submit(new CacheRetire());
    }

    private static void fulfillDocInfo(DocUnit docUnit) {
        String content = Optional.ofNullable(docUnit.getContent()).orElse("");
        docUnit.setVersion(System.currentTimeMillis());
        docUnit.setHash(XXHash.getXXHash(content));
        docUnit.setDocLength(content.length());
        docUnit.setLoad(false);
    }

    private static int findBitPosition(String id) {
        return id.hashCode() % CACHE_SEED;
    }

    private Predicate<DocUnit> filterDocByBitMap() {
        return doc -> 0 == bitmap[findBitPosition(doc.getDocId())];
    }

    private class CacheFlush extends Thread {
        @Override
        public void run() {
            int retry = 0;
            while (true) {
                try {
                    NodeInfo nodeInfo = NodeState.getAsObject("NODE", "NODE_INFO", null, NodeInfo.class);
                    if (ObjectUtils.isEmpty(nodeInfo)) {
                        log.warn("Node is not ready");
                        if (++retry < 20) {
                            Thread.sleep(5_000);
                        } else {
                            log.error("Node was dead");
                        }
                    } else {
                        retry = 0;
                        String nodeId = nodeInfo.getNodeId();
                        String cacheFilePattern = NodeConfig.getAsString("NODE#CACHE_FILE_PATH", CACHE_FILE_PATH_PATTERN);
                        String filePath = String.format(cacheFilePattern, nodeId);
                        if (StringUtils.isBlank(CACHE_FILE_PATH)) {
                            CACHE_FILE_PATH = filePath;
                        }

                        Set<String> cacheDocIds = ConcurrentHashMap.newKeySet();
                        Stream<String> stringStream = cache.values().stream()
                                .filter(filterDocByBitMap())
                                .peek(doc -> {
                                    String docId = doc.getDocId();
                                    cacheDocIds.add(docId);
                                    bitmap[findBitPosition(docId)] = 1;
                                })
                                .sorted(Comparator.comparingInt(doc -> findBitPosition(doc.getDocId())))
                                .map(doc -> String.format("%s:%s:%s"
                                        , doc.getDocId()
                                        , doc.getVersion()
                                        , JSON.toJSONString(doc)))
                                ;

                        if (!FileUtils.writeFile(filePath, stringStream)) {
                            log.error("Regular doc sync failure");
                            cacheDocIds.parallelStream()
                                    .forEach(docId -> bitmap[findBitPosition(docId)] = 0);
                        }

                        Thread.sleep(60_000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class CacheRetire extends Thread {
        @Override
        public void run() {
            long gapTime = System.currentTimeMillis() - CACHE_GAP;
            AtomicInteger cacheCounter = new AtomicInteger(0);
            cache.values().stream().filter(doc -> !filterDocByBitMap().test(doc))
                    .filter(doc -> doc.getVersion() < gapTime)
                    .peek(doc -> doc.setLoad(true))
                    .peek(doc -> doc.setContent(""))
            .forEach(doc -> cacheCounter.incrementAndGet());
            log.info("Cleaned {} doc cache", cacheCounter.intValue());
        }
    }
}
