package com.gjsk.mediagateway.common.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseResponseTest {

    @Test
    void okWithoutDataShouldUseSuccessCode() {
        BaseResponse<Void> response = BaseResponse.ok();

        assertThat(response.getCode()).isEqualTo(BaseResponse.SUCCESS);
        assertThat(response.getMessage()).isEqualTo("ok");
        assertThat(response.getData()).isNull();
        assertThat(response.getExtra()).isNull();
    }

    @Test
    void okWithDataShouldCarryData() {
        BaseResponse<String> response = BaseResponse.ok("value");

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo("value");
    }

    @Test
    void okWithExtraShouldCarryExtraMetadata() {
        BaseResponse<List<String>> response = BaseResponse.ok(List.of("a"), Map.of("debug", true));

        assertThat(response.getData()).containsExactly("a");
        assertThat(response.getExtra()).isEqualTo(Map.of("debug", true));
    }

    @Test
    void errorShouldCarryCodeAndMessageWithoutData() {
        BaseResponse<Void> response = BaseResponse.error(400, "参数错误");

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).isEqualTo("参数错误");
        assertThat(response.getData()).isNull();
    }
}
