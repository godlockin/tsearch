package com.st.tsearch;

import com.st.tsearch.common.utils.DataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class DataUtilsPagingFuncTest {

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
     * Test the over size cases for the paging function
     */
    @Test
    public void testOverSizePaging() {

        int pageIndex = 1;
        int pageSize = 10;
        List<String> targetList = DataUtils.handlePaging(pageIndex, pageSize, testList);

        Assert.notEmpty(targetList, "Can't handle over size page intercept");

        Assert.isTrue(testList.size() == targetList.size(), "Can't handle over size page intercept");

        Assert.isTrue("1".equalsIgnoreCase(targetList.get(0)), "Can't handle normal page intercept");

        Assert.isTrue("2".equalsIgnoreCase(targetList.get(1)), "Can't handle normal page intercept");

        Assert.isTrue("3".equalsIgnoreCase(targetList.get(2)), "Can't handle normal page intercept");
    }

    /**
     * Test for the empty page param
     */
    @Test
    public void testEmptyPage() {

        int pageIndex = 1;
        int pageSize = 0;
        List<String> targetList = DataUtils.handlePaging(pageIndex, pageSize, testList);

        Assert.isTrue(targetList.isEmpty(), "Can't handle empty page");
    }

    /**
     * Test whether the start index support both 0 and 1
     */
    @Test
    public void testStartIndexCompatible() {

        int pageIndex = 0;
        int pageSize = 10;
        List<String> targetList = DataUtils.handlePaging(pageIndex, pageSize, testList);

        Assert.notEmpty(targetList, "Can't handle over size page intercept");

        Assert.isTrue(testList.size() == targetList.size(), "Can't handle over size page intercept");

        Assert.isTrue("1".equalsIgnoreCase(targetList.get(0)), "Can't handle normal page intercept");
    }

    /**
     * Test the paging cases
     */
    @Test
    public void testPaging() {

        // start page
        int pageIndex = 1;
        int pageSize = 1;
        List<String> targetList = DataUtils.handlePaging(pageIndex, pageSize, testList);

        Assert.notEmpty(targetList, "Can't handle page intercept");

        Assert.isTrue(1 == targetList.size(), "Can't handle page intercept");

        Assert.isTrue("1".equalsIgnoreCase(targetList.get(0)), "Can't handle normal page intercept");

        // normal paging
        pageIndex = 2;
        targetList = DataUtils.handlePaging(pageIndex, pageSize, testList);

        Assert.notEmpty(targetList, "Can't handle page intercept");

        Assert.isTrue(1 == targetList.size(), "Can't handle page intercept");

        Assert.isTrue("2".equalsIgnoreCase(targetList.get(0)), "Can't handle normal page intercept");

        // over paging
        pageIndex = 4;
        targetList = DataUtils.handlePaging(pageIndex, pageSize, testList);

        Assert.isTrue(targetList.isEmpty(), "Can't handle page intercept");
    }
}
