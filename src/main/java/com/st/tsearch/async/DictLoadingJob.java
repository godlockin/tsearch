package com.st.tsearch.async;

import com.st.tsearch.common.Dictionary;
import com.st.tsearch.common.NodeConfig;
import com.st.tsearch.common.utils.ExtraCollectionUtils;
import com.st.tsearch.common.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@DependsOn({"clusterManager"})
public class DictLoadingJob {

    @Value("${DICT_FILE_PATH:./data/dict/dict.txt}")
    private String FILE_PATH;

    @PostConstruct
    void init() {
        String filePath = NodeConfig.getAsString("NODE#DICT_PATH", FILE_PATH);
        loadDictItems(filePath);
    }

    private void loadDictItems(String filePath) {
        if (!FileUtils.fileExists(filePath)) {
            log.error("No dict file found");
            return;
        }

        List<String> dictLines = FileUtils.loadFileContent(filePath);
        AtomicInteger tokenCounter = new AtomicInteger(0);
        Optional.of(dictLines)
                .filter(ExtraCollectionUtils::isNotEmpty)
                .map(Collection::stream)
                .ifPresent(stream ->
                        stream.filter(Dictionary::addToken)
                                .forEach(token -> tokenCounter.incrementAndGet()));

        int tokenCount = tokenCounter.intValue();
        log.info("Loaded {} token from [{}]", tokenCount, filePath);
        NodeConfig.set("NODE:DICT_READY", tokenCount > 0);
    }
}
