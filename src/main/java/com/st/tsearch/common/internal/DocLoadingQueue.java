package com.st.tsearch.common.internal;

import com.st.tsearch.model.localData.FileUnit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
public class DocLoadingQueue {
    private static final BlockingQueue<FileUnit> queue = new ArrayBlockingQueue<>(100);

    public static boolean push(FileUnit item) {
        return queue.offer(item);
    }

    public static int drainTo(List<FileUnit> list, Integer maxSize) {
        return queue.drainTo(list, Optional.ofNullable(maxSize).orElse(100));
    }
}
