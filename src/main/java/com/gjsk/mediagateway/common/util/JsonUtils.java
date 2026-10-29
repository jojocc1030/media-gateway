package com.gjsk.mediagateway.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;

/**
 * JSON 通用工具类，基于 Jackson。
 *
 * <p>静态 {@link ObjectMapper} 全局复用，仅用于内部序列化（Redis、日志、测试）；
 * 对外 HTTP 序列化由 Spring MVC 的 ObjectMapper 负责（全局 snake_case）。</p>
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtils() {
    }

    /**
     * 对象序列化为 JSON 字符串。
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串反序列化为指定类型对象。
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串反序列化为泛型对象（如 {@code List<User>}）。
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串反序列化为 {@code Map<String, Object>}。
     */
    public static Map<String, Object> toMap(String json) {
        return fromJson(json, new TypeReference<>() {
        });
    }

    /**
     * JSON 字符串反序列化为 {@code List<T>}。
     */
    public static <T> List<T> toList(String json, Class<T> elementClass) {
        return fromJson(json, new TypeReference<>() {
        });
    }

    /**
     * 获取底层 ObjectMapper，供需要直接使用的场景。
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
