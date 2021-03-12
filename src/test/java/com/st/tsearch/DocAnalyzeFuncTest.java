package com.st.tsearch;

import com.st.tsearch.async.DictLoadingJob;
import com.st.tsearch.service.analysis.DataAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;

public class DocAnalyzeFuncTest {

    @BeforeEach
    public void dataInit() {
        String dictPath = "./data/dict/dict.txt";

        try {
            DictLoadingJob job = new DictLoadingJob();
            Method method = job.getClass().getDeclaredMethod("loadDictItems", String.class);
            method.setAccessible(true);
            method.invoke(job, dictPath);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testNormalContentAnalyze() {
        String content = "中国上海欢迎你";
        dataInit();

        List<String> tokens = DataAnalyzer.analyzeContent(content);

        Assert.isTrue(!CollectionUtils.isEmpty(tokens), "Can't find correct tokens");

        Assert.isTrue(2 == tokens.size(), "Can't find correct tokens");

        Assert.isTrue(tokens.contains("中国") && tokens.contains("上海"), "Can't find correct tokens");

        Assert.isTrue(!(tokens.contains("欢迎") || tokens.contains("你")), "Can't find correct tokens");
    }

    @Test
    public void testCantAnalyzeBeforeDictionaryInit() {
        String content = "中国上海欢迎你";
        List<String> tokens = DataAnalyzer.analyzeContent(content);

        Assert.isTrue(CollectionUtils.isEmpty(tokens), "Shouldn't get the tokens without dictionary init");
    }

}
