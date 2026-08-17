package com.jinbo.myerp.config;

import com.jinbo.myerp.service.SaleService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaleLockStrategyConfigTest {

    private final SaleService optimistic = Mockito.mock(SaleService.class);
    private final SaleService pessimistic = Mockito.mock(SaleService.class);

    @Test
    void resolve_optimistic_returnsOptimistic() {
        assertThat(SaleLockStrategyConfig.resolve("optimistic", optimistic, pessimistic))
                .isSameAs(optimistic);
    }

    @Test
    void resolve_pessimistic_returnsPessimistic() {
        assertThat(SaleLockStrategyConfig.resolve("pessimistic", optimistic, pessimistic))
                .isSameAs(pessimistic);
    }

    @Test
    void resolve_isCaseInsensitive() {
        assertThat(SaleLockStrategyConfig.resolve("PESSIMISTIC", optimistic, pessimistic))
                .isSameAs(pessimistic);
    }

    /**
     * 오타를 조용히 기본값으로 넘기면, 비관적 락을 측정한다고 믿으면서 실제로는
     * 낙관적 락을 재게 된다 — 부하 비교 결과 전체가 무의미해지므로 기동을 실패시킨다.
     */
    @Test
    void resolve_unknownStrategy_throws() {
        assertThatThrownBy(() -> SaleLockStrategyConfig.resolve("pessimistc", optimistic, pessimistic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pessimistc");
    }
}
