package com.st.tsearch.common.utils;

import com.st.tsearch.model.localData.FileUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@Component
public class FileUtils {

    public static boolean fileExists(String path) {
        return new File(path).exists();
    }

    public static List<FileUnit> loadFilesPathFromFolder(String folderPath, String filePrefix, String fileSuffix) {
        List<FileUnit> fileUnits = new ArrayList<>();
        File file = new File(folderPath);
        if (!file.exists() || !file.isDirectory()) {
            log.error("Illegal file for folderPath:[{}]", folderPath);
            return fileUnits;
        }

        File[] files = file.listFiles();
        if (null == files || 0 == file.length()) {
            log.error("Empty folder");
            return fileUnits;
        }

        boolean ignorePrefix = StringUtils.isBlank(filePrefix);
        boolean ignoreSuffix = StringUtils.isBlank(fileSuffix);
        String prefix = folderPath + "/" + filePrefix;
        Stream.of(files)
                .filter(childFile -> !childFile.isDirectory())
                .filter(childFile -> ignorePrefix || childFile.getPath().startsWith(prefix))
                .filter(childFile -> ignoreSuffix || childFile.getPath().endsWith(fileSuffix))
                .map(childFile -> {
                    FileUnit fileUnit = new FileUnit();
                    String fileName = childFile.getName();
                    String title = fileName;
                    int split = fileName.lastIndexOf(".");
                    if (0 < split) {
                        title = fileName.substring(0, split);
                    }
                    fileUnit.setTitle(title);
                    fileUnit.setFileName(fileName);
                    fileUnit.setFilePath(childFile.getAbsolutePath());
                    return fileUnit;
                })
                .distinct()
                .forEach(fileUnits::add);
        log.info("Loaded {} files' info", fileUnits.size());
        return fileUnits;
    }

    public static List<String> loadFileContent(String path) {
        return loadFileContent(path, 0L);
    }

    public static List<String> loadFileContent(String path, long skip) {
        List<String> fileLines = new ArrayList<>();
        File file = new File(path);

        if (!file.exists() || file.isDirectory()) {
            log.error("Illegal file for path:[{}]", path);
            return fileLines;
        }

        skip = skip < 0 ? 0 : skip;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            log.info("Try to load data from:[{}]", path);
            reader.lines()
                    .filter(Objects::nonNull)
                    .filter(StringUtils::isNotBlank)
                    .skip(skip)
                    .map(String::trim)
                    .forEach(fileLines::add);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Got error during we load file:[{}], msg:[{}]", path, e);
        }
        return fileLines;
    }

    public static boolean writeFile(String path, Stream<String> stringStream) {
        File file = new File(path);

        if (!file.exists() || file.isDirectory()) {
            log.error("Illegal file for path:[{}]", path);
            return false;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            AtomicBoolean isSs = new AtomicBoolean(false);
            stringStream.forEach(str -> {
                try {
                    bw.write(str);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Got error during we write file:[{}], msg:[{}]", path, e);
                    isSs.set(true);
                }
            });

            return !isSs.get();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Got error during we write file:[{}], msg:[{}]", path, e);
            return false;
        }
    }
}
