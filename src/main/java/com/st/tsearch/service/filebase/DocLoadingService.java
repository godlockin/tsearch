package com.st.tsearch.service.filebase;

import com.st.tsearch.common.internal.DocLoadingQueue;
import com.st.tsearch.common.utils.FileUtils;
import com.st.tsearch.model.localData.DocLoadConfig;
import com.st.tsearch.model.localData.FileUnit;
import com.st.tsearch.service.IDocLoadingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class DocLoadingService implements IDocLoadingService {

    /**
     * Handle data loading process against the loading config
     *
     * @param config config for data loading process
     * @return whether the data loading job success
     */
    public boolean loadLocalDoc(DocLoadConfig config) {

        // do config validation
        if (!validateConfig(config)) {
            log.warn("Data loading config issue");
            return false;
        }

        // do data loading process
        if (!doDataLoading(config)) {
            log.warn("Data loading failure");
            return false;
        }

        return true;
    }

    /**
     * Load doc defined in config
     *
     * @param config data loading config bean
     * @return whether doc load success
     */
    private boolean doDataLoading(DocLoadConfig config) {
        log.info("Try to load documents for config:[{}]", config);

        List<FileUnit> filePaths = FileUtils.loadFilesPathFromFolder(
                config.getFolderPath()
                , config.getFilePrefix()
                , config.getFileSuffix());
        if (CollectionUtils.isEmpty(filePaths)) {
            log.warn("No target file found");
            return false;
        }

        // TODO change this part of logic into multi-thread with queue
        AtomicInteger fileCounter = new AtomicInteger(0);
        filePaths.parallelStream().peek(x -> fileCounter.incrementAndGet()).forEach(DocLoadingQueue::push);
        log.info("Loaded {} files from path[{}]", fileCounter.intValue(), config.getFolderPath());
        return true;
    }

    /**
     * File base document loading config validation, check if target folder exists so far
     *
     * @param config data loading config bean
     * @return whether validation pass
     */
    private boolean validateConfig(DocLoadConfig config) {
        String filePath = config.getFolderPath();
        return StringUtils.isNotBlank(filePath) && FileUtils.fileExists(filePath);
    }
}
