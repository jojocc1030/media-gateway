package com.gjsk.mediagateway.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void shouldGenerateTraceIdAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo/items");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> insideTraceId = new AtomicReference<>();

        FilterChain chain = (req, res) -> insideTraceId.set(MDC.get(TraceIdFilter.MDC_TRACE_ID));
        filter.doFilter(request, response, chain);

        assertThat(insideTraceId.get()).matches("[0-9a-f]{32}");
        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(insideTraceId.get());
        assertThat(MDC.get(TraceIdFilter.MDC_TRACE_ID)).isNull();
    }

    @Test
    void shouldReuseValidIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo/items");
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "abcdef123456");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> insideTraceId = new AtomicReference<>();

        FilterChain chain = (req, res) -> insideTraceId.set(MDC.get(TraceIdFilter.MDC_TRACE_ID));
        filter.doFilter(request, response, chain);

        assertThat(insideTraceId.get()).isEqualTo("abcdef123456");
        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("abcdef123456");
    }

    @Test
    void shouldReplaceIllegalIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo/items");
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "bad id!");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> insideTraceId = new AtomicReference<>();

        FilterChain chain = (req, res) -> insideTraceId.set(MDC.get(TraceIdFilter.MDC_TRACE_ID));
        filter.doFilter(request, response, chain);

        assertThat(insideTraceId.get()).matches("[0-9a-f]{32}").isNotEqualTo("bad id!");
    }
}
