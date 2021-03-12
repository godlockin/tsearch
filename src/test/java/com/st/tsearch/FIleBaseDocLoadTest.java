package com.st.tsearch;

import com.st.tsearch.common.internal.DocLoadingQueue;
import com.st.tsearch.model.localData.DocLoadConfig;
import com.st.tsearch.model.localData.FileUnit;
import com.st.tsearch.service.filebase.DocLoadingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FIleBaseDocLoadTest {

    private DocLoadingService service;
    private List<String> includeFileNames;
    private List<String> excludeFileNames;

    @BeforeEach
    public void dataInit() {
        service = new DocLoadingService();

        includeFileNames = new ArrayList<>();
        includeFileNames.add("中信证券喊目标价3000元贵州茅台节后市值蒸发7135亿.txt");
        includeFileNames.add("句子很短的测试文件");
        includeFileNames.add("知乎十年路口：承压商业化变现.txt");
        includeFileNames.add("知乎启动美股IPO！两年亏15亿，腾讯、快手股东阵容豪华.txt");
        includeFileNames.add("谢邀，知乎即将IPO登陆美股.txt");
        includeFileNames.add("贵州茅台酒股份有限公司关于董事辞职的公告.txt");

        excludeFileNames = new ArrayList<>();
        excludeFileNames.add("file_not_exists.txt");
    }

    @AfterEach
    public void testDataClean() {
        includeFileNames = new ArrayList<>();
        excludeFileNames = new ArrayList<>();
    }

    /**
     * Test normal cases
     */
    @Test
    public void testDocLoadFromFile() {
        String docFolderPath = "./data/doc";
        DocLoadConfig config = new DocLoadConfig();
        config.setFolderPath(docFolderPath);

        String errorMessage = "Can't handle normal file loading";

        boolean isSuccess = service.loadLocalDoc(config);
        Assert.isTrue(isSuccess, errorMessage);

        List<FileUnit> fileUnits = new ArrayList<>();
        DocLoadingQueue.drainTo(fileUnits, 100);

        Assert.notEmpty(fileUnits, errorMessage);
        Assert.isTrue(6 == fileUnits.size(), errorMessage);

        List<String> foundFiles = fileUnits.stream()
                .map(FileUnit::getFileName)
                .collect(Collectors.toList());

        includeFileNames.forEach(fileName -> Assert.isTrue(foundFiles.contains(fileName), errorMessage));
        excludeFileNames.forEach(fileName -> Assert.isTrue(!foundFiles.contains(fileName), errorMessage));
    }

    /**
     * Test not exists folder
     */
    @Test
    public void testTargetFolderNotExists() {
        String docFolderPath = "not_exists";
        DocLoadConfig config = new DocLoadConfig();
        config.setFolderPath(docFolderPath);

        String errorMessage = "Can't handle not exists folder";

        boolean isSuccess = service.loadLocalDoc(config);
        Assert.isTrue(!isSuccess, errorMessage);

        List<FileUnit> fileUnits = new ArrayList<>();
        DocLoadingQueue.drainTo(fileUnits, 100);

        Assert.isTrue(fileUnits.isEmpty(), errorMessage);
    }

    /**
     * Test for files' suffix
     */
    @Test
    public void testFileFilterOfSuffix() {
        String docFolderPath = "./data/doc";
        String existsSuffix = "txt";

        DocLoadConfig config = new DocLoadConfig();
        config.setFolderPath(docFolderPath);
        config.setFileSuffix(existsSuffix);

        String errorMessage = "Can't handle file suffix filter";

        boolean isSuccess = service.loadLocalDoc(config);
        Assert.isTrue(isSuccess, errorMessage);

        List<FileUnit> fileUnits = new ArrayList<>();
        DocLoadingQueue.drainTo(fileUnits, 100);
        Assert.isTrue(5 == fileUnits.size(), errorMessage);

        List<String> foundFiles = fileUnits.stream()
                .map(FileUnit::getFileName)
                .collect(Collectors.toList());
        Assert.isTrue(!foundFiles.contains("句子很短的测试文件"), errorMessage);

        String notExistsSuffix = "png";
        config.setFileSuffix(notExistsSuffix);

        isSuccess = service.loadLocalDoc(config);
        Assert.isTrue(!isSuccess, errorMessage);

        fileUnits = new ArrayList<>();
        DocLoadingQueue.drainTo(fileUnits, 100);
        Assert.isTrue(fileUnits.isEmpty(), errorMessage);
    }

    @Test
    public void testFileFilterOfPrefix() {
        String docFolderPath = "./data/doc";
        String existsPrefix = "知乎";

        DocLoadConfig config = new DocLoadConfig();
        config.setFolderPath(docFolderPath);
        config.setFilePrefix(existsPrefix);

        String errorMessage = "Can't handle file prefix filter";

        boolean isSuccess = service.loadLocalDoc(config);
        Assert.isTrue(isSuccess, errorMessage);

        List<FileUnit> fileUnits = new ArrayList<>();
        DocLoadingQueue.drainTo(fileUnits, 100);
        Assert.isTrue(2 == fileUnits.size(), errorMessage);

        List<String> foundFiles = fileUnits.stream()
                .map(FileUnit::getFileName)
                .collect(Collectors.toList());

        Assert.isTrue(foundFiles.contains("知乎十年路口：承压商业化变现.txt"), errorMessage);
        Assert.isTrue(foundFiles.contains("知乎启动美股IPO！两年亏15亿，腾讯、快手股东阵容豪华.txt"), errorMessage);

        String notExistsPrefix = "not_exists_prefix";
        config.setFilePrefix(notExistsPrefix);

        isSuccess = service.loadLocalDoc(config);
        Assert.isTrue(!isSuccess, errorMessage);

        fileUnits = new ArrayList<>();
        DocLoadingQueue.drainTo(fileUnits, 100);
        Assert.isTrue(fileUnits.isEmpty(), errorMessage);
    }

}
