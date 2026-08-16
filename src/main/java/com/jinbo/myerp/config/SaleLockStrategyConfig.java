package com.jinbo.myerp.config;

import com.jinbo.myerp.service.OptimisticLockSaleService;
import com.jinbo.myerp.service.PessimisticLockSaleService;
import com.jinbo.myerp.service.SaleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 매출 등록에 쓸 락 전략을 설정으로 고른다(기본: 낙관적 락).
 *
 * 4단계에서 전략 패턴으로 두 구현을 만들어뒀지만 컨트롤러가 낙관적 락으로 고정되어
 * 있어 비관적 락은 REST API로 접근할 수 없었고, 그래서 같은 부하를 두 전략에
 * 걸어보는 비교가 불가능했다(k6/LOAD_TEST_PLAN.md A1).
 *
 * 쿼리파라미터(?lockMode=...)로 런타임에 바꾸는 방법도 있었지만, 테스트용
 * 파라미터가 프로덕션 API에 영구히 남기 때문에 택하지 않았다. 설정 방식은
 * 프로파일을 바꾸려면 재기동해야 하는데, 부하 테스트에서는 JIT/커넥션 풀 상태가
 * 초기화되는 편이 오히려 조건 격리에 유리하다.
 *
 * 두 서비스 빈은 그대로 남는다 — SaleConcurrencyIntegrationTest가 두 구현을 직접
 * 주입해서 각각 검증하기 때문이다. 여기서는 컨트롤러가 주입받을 @Primary만 정한다.
 */
@Configuration
public class SaleLockStrategyConfig {

    public static final String OPTIMISTIC = "optimistic";
    public static final String PESSIMISTIC = "pessimistic";

    @Bean
    @Primary
    public SaleService primarySaleService(
            @Value("${myerp.sale.lock-strategy:" + OPTIMISTIC + "}") String strategy,
            OptimisticLockSaleService optimisticLockSaleService,
            PessimisticLockSaleService pessimisticLockSaleService) {

        return resolve(strategy, optimisticLockSaleService, pessimisticLockSaleService);
    }

    /**
     * 모르는 값이면 기본값으로 넘어가지 않고 기동을 실패시킨다.
     * 오타(pessimistc 등)를 조용히 무시하면 비관적 락을 잰다고 믿으면서 실제로는
     * 낙관적 락을 측정하게 되고, 비교 결과 전체가 무의미해진다.
     */
    static SaleService resolve(String strategy, SaleService optimistic, SaleService pessimistic) {
        if (OPTIMISTIC.equalsIgnoreCase(strategy)) {
            return optimistic;
        }
        if (PESSIMISTIC.equalsIgnoreCase(strategy)) {
            return pessimistic;
        }
        throw new IllegalArgumentException(
                "알 수 없는 락 전략입니다: '" + strategy + "' (사용 가능: " + OPTIMISTIC + ", " + PESSIMISTIC + ")");
    }
}
