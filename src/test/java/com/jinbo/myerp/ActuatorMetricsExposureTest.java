package com.jinbo.myerp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 부하 테스트 중 서버 내부를 관측하기 위한 actuator 지표가
 * (1) 아무나 볼 수 없고, (2) 실제로 필요한 것들이 노출되는지 검증한다.
 *
 * k6는 클라이언트 측(응답시간/상태코드)만 볼 수 있어서 "왜 느려졌는가"가 추측이 된다.
 * 여기서 검증하는 지표들이 그 추측을 실측으로 바꾸는 근거이므로, 나중에 누군가
 * exposure 설정을 줄이면 이 테스트가 먼저 깨져야 한다.
 *
 * tomcat.threads.* 는 실제 서블릿 컨테이너가 떠 있어야 등록되는데 이 테스트는
 * MOCK 환경이라 여기서 검증하지 않는다 — bootRun 후 실제 확인 대상.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorMetricsExposureTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void metrics_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void metrics_authenticated_exposesLoadTestMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItems(
                        "hikaricp.connections.active",
                        "hikaricp.connections.pending",
                        "hikaricp.connections.acquire",
                        "jvm.memory.used",
                        "jvm.threads.live"
                )));
    }

    @Test
    @WithMockUser
    void metrics_hikariPending_returnsMeasurement() throws Exception {
        mockMvc.perform(get("/actuator/metrics/hikaricp.connections.pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value").exists());
    }

    /**
     * 필요한 것만 노출한다(include: health,metrics). env/beans/configprops처럼
     * 설정값·빈 목록이 통째로 드러나는 엔드포인트는 부하 측정에 필요 없으므로
     * 열지 않는다 — 인증이 걸려 있어도 노출 범위 자체를 좁혀둔다.
     *
     * 매핑되지 않은 경로이므로 404가 정상이다(GlobalExceptionHandlerTest 참고 —
     * 이 테스트를 작성하다 매핑 없는 URL이 500을 반환하던 결함을 발견해 고쳤다).
     */
    @Test
    @WithMockUser
    void env_isNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }
}
