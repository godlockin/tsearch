package com.st.tsearch.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NodeState {

    private static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> cache = new ConcurrentHashMap<>();

    public static void set(String key, String subKey, Object value) {
        set(key, subKey, value, true);
    }

    public static void set(String key, String subKey, Object value, boolean forceUpdate) {
        cache.putIfAbsent(key, new ConcurrentHashMap<>());
        if (forceUpdate) {
            cache.get(key).put(subKey, value);
        } else {
            cache.get(key).putIfAbsent(subKey, value);
        }
    }

    public static Long getAsLong(String key, String subKey, Object defaultValue) {
        return (Long) get(key, subKey, defaultValue, Long.class);
    }

    public static <T> T getAsObject(String key, String subKey, Object defaultValue, Class<T> tClass) {
        return (T) get(key, subKey, defaultValue, tClass);
    }

    public static Integer getAsInteger(String key, String subKey, Object defaultValue) {
        return (Integer) get(key, subKey, defaultValue, Integer.class);
    }

    public static String getAsString(String key, String subKey, Object defaultValue) {
        return (String) get(key, subKey, defaultValue, String.class);
    }

    public static Set<String> getAsSet(String key, String subKey, Object defaultValue) {
        return (Set<String>) get(key, subKey, defaultValue, ConcurrentHashMap.class);
    }

    public static ConcurrentHashMap<String, Object> getAsConcurrentHashMap(String key, String subKey, Object defaultValue) {
        return (ConcurrentHashMap<String, Object>) get(key, subKey, defaultValue, ConcurrentHashMap.class);
    }

    public static Object get(String key, String subKey, Object defaultValue, Class<?> clazz) {
        assert StringUtils.isNotBlank(key) : "Empty key";

        Object _value = Optional.ofNullable(cache.get(key))
                .map(map -> map.get(subKey))
                .orElse(defaultValue);

        Object value = clazz.cast(_value);
        set(key, subKey, value);

        return value;
    }

}
