package com.jinbo.myerp.service;

import com.jinbo.myerp.controller.SaleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 락 전략을 설정으로 바꿀 수 있는지, 그리고 그 설정이 실제로 컨트롤러가 쓰는
 * 구현까지 도달하는지 검증한다.
 *
 * 4단계에서 전략 패턴으로 두 구현을 만들어뒀지만 SaleController가
 * @Qualifier("optimisticLockSaleService")로 고정되어 있어 비관적 락은 HTTP로
 * 접근할 수 없었다. 그래서 k6로 두 전략을 비교하는 것이 불가능했다
 * (k6/LOAD_TEST_PLAN.md A1).
 *
 * 여기서 "컨트롤러가 실제로 들고 있는 구현"을 직접 확인하는 이유: 부하 비교의
 * 신뢰성이 전적으로 여기에 달려 있기 때문이다. 설정만 바꿨다고 믿고 측정했는데
 * 실제로는 계속 낙관적 락을 재고 있었다면 결과 전체가 무의미해진다.
 */
@SpringBootTest
@TestPropertySource(properties = "myerp.sale.lock-strategy=pessimistic")
class SaleLockStrategyWiringTest {

    @Autowired
    private SaleController saleController;

    @Test
    void pessimisticProperty_controllerUsesPessimisticImplementation() {
        Object used = ReflectionTestUtils.getField(saleController, "saleService");

        assertThat(used).isInstanceOf(PessimisticLockSaleService.class);
    }
}
