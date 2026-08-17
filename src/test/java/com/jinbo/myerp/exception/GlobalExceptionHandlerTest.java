package com.jinbo.myerp.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 매핑되지 않은 URL은 GlobalExceptionHandler의 Exception.class catch-all에 잡혀
 * 500으로 처리되고 있었다(ActuatorMetricsExposureTest 작성 중 발견). 클라이언트가
 * URL을 잘못 호출한 것과 서버 결함을 구분할 수 없게 만들고, k6/모니터링에서 5xx
 * 비율을 서버 결함 지표로 쓰는 전제도 깨뜨린다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void unmappedUrl_returns404NotServerError() throws Exception {
        mockMvc.perform(get("/api/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
