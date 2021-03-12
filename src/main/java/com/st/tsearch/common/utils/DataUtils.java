package com.st.tsearch.common.utils;

import com.st.tsearch.common.constants.Constants.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
public class DataUtils {

    private static final Random random = new Random(100);

    public static String initId() {
        return XXHash.getXXHash(Long.toString(System.nanoTime()) + random.nextInt());
    }

    public static String buildKey(String first, String second) {
        return buildKey(first, second, SysConfig.KEY_POSITION);
    }

    public static String buildKey(String first, String second, String delimiter) {
        return String.join(
                Optional.ofNullable(delimiter).orElse(SysConfig.KEY_POSITION)
                , Optional.ofNullable(first).orElse("")
                , Optional.ofNullable(second).orElse("")
        );
    }

    public static MutablePair<String, String> splitKey(String key) {
        return splitKey(key, SysConfig.KEY_POSITION);
    }

    public static MutablePair<String, String> splitKey(String key, String delimiter) {
        if (StringUtils.isBlank(key)) {
            return MutablePair.of("", "");
        }

        String[] items;
        if (!(StringUtils.isNotBlank(delimiter) && key.contains(delimiter))) {
            items = new String[]{ key, "" };
        } else {
            items = key.split(delimiter);
        }

        return MutablePair.of(items[0], items[1]);
    }

    /**
     * Looping function to handle the iterable collection with increasing the index
     *
     * @param maxIndex last index of the iterable collection
     * @param elements iterable collection
     * @param action handle action for each element
     * @param <E> data type of the iterable elements
     */
    public static <E> void forEach(Integer maxIndex, Iterable<? extends E> elements, BiConsumer<Integer, ? super E> action) {
        Objects.requireNonNull(elements);
        Objects.requireNonNull(action);
        int index = 0;
        for (E element : elements) {
            action.accept(index++, element);
            if (maxIndex > 0 && maxIndex < index) {
                break;
            }
        }
    }

    /**
     * Handle the paging list data
     * @param pageIndex index of page
     * @param pageSize data number of each page
     * @param dataList target data list
     * @param <T> data type of the data list
     * @return intercept data list
     */
    public static <T> List<T> handlePaging(int pageIndex, int pageSize, List<T> dataList) {
        int resultSize = dataList.size();
        pageIndex = Math.max(1, pageIndex);
        int start = (pageIndex - 1) * pageSize;
        int end = Math.min(resultSize, pageIndex * pageSize);
        return (start <= end) ? dataList.subList(start, end) : new ArrayList<>();
    }
}