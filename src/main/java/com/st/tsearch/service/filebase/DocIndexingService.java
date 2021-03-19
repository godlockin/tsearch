package com.st.tsearch.service.filebase;

import com.alibaba.fastjson.JSON;
import com.st.tsearch.common.internal.DocLoadingQueue;
import com.st.tsearch.common.utils.DataUtils;
import com.st.tsearch.common.utils.FileUtils;
import com.st.tsearch.common.utils.XXHash;
import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.localData.FileUnit;
import com.st.tsearch.service.distribute.DocSaver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class DocIndexingService {

    @Value("${DOC_LOAD_FILE_BASE_GROUP_SIZE:100}")
    private Integer GROUP_SIZE;

    private static ThreadPoolTaskExecutor executor;
    static {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(1024);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
    }

    @Scheduled(cron = "0/10 * * * * ?")
    void handleFiles() {
        List<FileUnit> list = new ArrayList<>();
        DocLoadingQueue.drainTo(list, GROUP_SIZE);
        if (CollectionUtils.isEmpty(list)) {
            log.debug("No files' info found");
            return;
        }

        AtomicInteger docCounter = new AtomicInteger(0);

        CompletableFuture[] array = list.parallelStream()
                .filter(file -> FileUtils.fileExists(file.getFilePath()))
                .map(file -> CompletableFuture.runAsync(() -> {
                    // load data and save into queue
                    long timestamp = System.nanoTime();
                    String docId = DataUtils.initId();
                    List<String> sentences = FileUtils.loadFileContent(file.getFilePath());

                    // store the whole doc for small benchmark
                    String docContent = String.join("\n", sentences);
                    String hash = XXHash.getXXHash(docContent);

                    DocUnit docUnit = new DocUnit(docId
                            , hash
                            , docContent.length()
                            , docContent
                            , timestamp
                            , false
                    );

                    if (DocSaver.push(docUnit)) {
                        docCounter.incrementAndGet();
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(array).whenCompleteAsync((__, e) -> {
            if (ObjectUtils.isNotEmpty(e)) {
                log.error("Error happens during we index the docs with error: {}", e);
            } else {
                log.info("Success handel {} files", list.size());
            }
        }, executor).join();
        log.info("Pushed {} doc from {} files", docCounter.intValue(), list.size());
    }

}
