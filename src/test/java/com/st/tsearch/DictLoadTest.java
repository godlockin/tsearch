package com.st.tsearch;

import com.st.tsearch.async.DictLoadingJob;
import com.st.tsearch.common.Dictionary;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

public class DictLoadTest {

    /**
     * Test for load dict from file
     */
    @Test
    public void testDictLoadFromFile() {
        String dictPath = "./data/dict/dict.txt";

        try {
            DictLoadingJob job = new DictLoadingJob();
            Method method = job.getClass().getDeclaredMethod("loadDictItems", String.class);
            method.setAccessible(true);
            method.invoke(job, dictPath);
        } catch (Exception e){
            e.printStackTrace();
        }

        Assert.isTrue(2 == Dictionary.getMinToken(), "Wrong number of min token length got");

        Assert.isTrue(12 == Dictionary.getTokenCount(), "Wrong number of tokens loaded");

        Assert.isTrue(Dictionary.isContains("中国"), "Missing token of '中国' in dictionary");

        Assert.isTrue(!Dictionary.isContains("美国"), "Unexpected token of '美国' saved");

        Assert.isTrue(Dictionary.addToken("我"), "Add new token '我' failure");

        Assert.isTrue(Dictionary.addToken("美国"), "Add new token '美国' failure");

        Assert.isTrue(1 == Dictionary.getMinToken(), "Wrong number of min token length got");

        Assert.isTrue(14 == Dictionary.getTokenCount(), "Wrong number of tokens loaded");
    }


}
