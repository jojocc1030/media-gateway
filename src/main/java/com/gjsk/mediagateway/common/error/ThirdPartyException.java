package com.gjsk.mediagateway.common.error;

/**
 * 上游服务调用失败（502）。
 *
 * <p>对外消息不包含上游地址、凭据等敏感信息；详细原因写日志。</p>
 */
public class ThirdPartyException extends BusinessException {

    public ThirdPartyException(String message) {
        super(ErrorCode.BAD_GATEWAY.getCode(), message);
    }

    public ThirdPartyException(String service, int upstreamStatus) {
        this("上游服务调用失败: " + service + ", status=" + upstreamStatus);
    }
}
