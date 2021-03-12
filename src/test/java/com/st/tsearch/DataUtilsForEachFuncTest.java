package com.st.tsearch;

import com.st.tsearch.common.utils.DataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class DataUtilsForEachFuncTest {

    private List<String> testList;

    /**
     * Init test list for function's test
     */
    @BeforeEach
    public void dataInit() {
        testList = new ArrayList<>();
        testList.add("1");
        testList.add("2");
        testList.add("3");
    }

    /**
     * Clean up the test data
     */
    @AfterEach
    public void dataClean() {
        testList = new ArrayList<>();
    }

    /**
     * Test normal foreach function
     */
    @Test
    public void testForEach() {

        List<Integer> idxList = new ArrayList<>();
        List<String> elementList = new ArrayList<>();
        DataUtils.forEach(testList.size(), testList, (index, element) -> {
            idxList.add(index);
            elementList.add(element);
        });

        String errorMessage = "Can't handle normal foreach";

        Assert.notEmpty(idxList, errorMessage);
        Assert.notEmpty(elementList, errorMessage);

        Assert.isTrue(testList.size() == idxList.size(), errorMessage);
        Assert.isTrue(testList.size() == elementList.size(), errorMessage);

        int testSize = testList.size();
        for (int i = 0; i < testSize; i ++) {
            Assert.isTrue(i == idxList.get(i), errorMessage);

            Assert.isTrue(testList.get(i).equals(elementList.get(i)), errorMessage);
        }
    }

    /**
     * Test the foreach func with limitation of index
     */
    @Test
    public void testElementsLimitation() {
        int maxIdx = 1;
        int nextIdx = maxIdx + 1;
        List<Integer> idxList = new ArrayList<>();
        List<String> elementList = new ArrayList<>();
        DataUtils.forEach(maxIdx, testList, (index, element) -> {
            idxList.add(index);
            elementList.add(element);
        });

        String errorMessage = "Can't handle elements limit foreach";

        Assert.notEmpty(idxList, errorMessage);
        Assert.notEmpty(elementList, errorMessage);

        Assert.isTrue(nextIdx == idxList.size(), errorMessage);
        Assert.isTrue(nextIdx == elementList.size(), errorMessage);

        int testSize = testList.size();
        int i = 0;
        try {
            for (; i < testSize; i++) {
                Assert.isTrue(i == idxList.get(i), errorMessage);

                Assert.isTrue(testList.get(i).equals(elementList.get(i)), errorMessage);
            }
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            Assert.isTrue(i == nextIdx, errorMessage);
        } catch (Exception e) {
            Assert.isTrue(false, errorMessage);
            throw e;
        }
    }
}
