package com.st.tsearch;

import com.st.tsearch.async.DictLoadingJob;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Method;

public class DocSearchFuncTest {

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

}
