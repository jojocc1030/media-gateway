package com.gjsk.mediagateway.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gjsk.mediagateway.common.web.GlobalExceptionHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 测试用 MockMvc 构造器。
 *
 * <p>standalone 模式默认使用 camelCase ObjectMapper，这里显式装配与生产一致的
 * SNAKE_CASE 命名策略，保证测试断言覆盖真实对外契约。</p>
 */
public final class MvcTestSupport {

    private MvcTestSupport() {
    }

    /**
     * 构造带全局异常处理的 standalone MockMvc。
     *
     * @param controllers 被测控制器实例
     */
    public static MockMvc standaloneMockMvc(Object... controllers) {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }
}
