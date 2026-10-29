package com.gjsk.mediagateway.common.web;

import com.gjsk.mediagateway.common.util.IdUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 请求链路追踪过滤器。
 *
 * <p>读取或生成 {@code X-Request-Id}，写入 MDC 的 {@code traceId} 并回写响应头，
 * 供日志关联与上下游透传。请求结束后清理 MDC。</p>
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /** 链路追踪请求/响应头。 */
    public static final String TRACE_ID_HEADER = "X-Request-Id";
    /** MDC key。 */
    public static final String MDC_TRACE_ID = "traceId";

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (header != null && TRACE_ID_PATTERN.matcher(header).matches()) {
            return header;
        }
        return IdUtils.newRequestId();
    }
}
