package com.jinbo.myerp.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    /**
     * 읽을 수 없는 요청 본문(깨진 JSON, 잘못된 인코딩, 타입 불일치)도 같은 catch-all에
     * 걸려 500이 나고 있었다. 인가 작업 중 curl로 한글이 깨진 본문을 보냈다가 발견했다.
     * 클라이언트가 보낸 값의 문제이므로 400이어야 하고, 아니면 5xx 비율을 서버 결함
     * 지표로 쓰는 전제가 또 깨진다.
     */
    @Test
    @WithMockUser
    void malformedJsonBody_returns400NotServerError() throws Exception {
        mockMvc.perform(post("/api/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"닫히지 않은 문자열"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser
    void wrongTypeInJsonBody_returns400NotServerError() throws Exception {
        // categorySubId는 숫자인데 문자열 객체를 보냈다.
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorySubId\": {\"nested\": 1}, \"name\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
