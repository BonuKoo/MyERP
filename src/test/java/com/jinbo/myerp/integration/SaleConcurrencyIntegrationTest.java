package com.jinbo.myerp.integration;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.domain.SaleItem;
import com.jinbo.myerp.domain.UserRole;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.OptimisticLockConflictException;
import com.jinbo.myerp.mapper.CategoryMainMapper;
import com.jinbo.myerp.mapper.CategorySubMapper;
import com.jinbo.myerp.mapper.CompanyInfoMapper;
import com.jinbo.myerp.mapper.CompanyUserMapper;
import com.jinbo.myerp.mapper.ItemMapper;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import com.jinbo.myerp.mapper.PartnerMapper;
import com.jinbo.myerp.service.SaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 로컬 MySQL을 대상으로 한 매출 확정 동시성 검증.
 * H2는 락 동작이 MySQL InnoDB와 다르므로(특히 SELECT...FOR UPDATE) 반드시 real DB로 검증한다.
 * ./gradlew integrationTest 로만 실행됨 (기본 test 태스크에서는 제외).
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
class SaleConcurrencyIntegrationTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    @Autowired
    private CategorySubMapper categorySubMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemSpecMapper itemSpecMapper;

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private CompanyInfoMapper companyInfoMapper;

    @Autowired
    private CompanyUserMapper companyUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("pessimisticLockSaleService")
    private SaleService pessimisticLockSaleService;

    @Autowired
    @Qualifier("optimisticLockSaleService")
    private SaleService optimisticLockSaleService;

    private Long partnerId;
    private Long companyInfoId;
    private Long userId;
    private Long categoryMainId;
    private Long categorySubId;
    private Long itemId;
    private Long itemSpecId;
    private final List<Long> saleIds = new ArrayList<>();

    @BeforeEach
    void setUpCommonFixtures() {
        LocalDateTime now = LocalDateTime.now();
        Partner partner = Partner.builder().name("동시성테스트거래처-" + System.nanoTime())
                .partnerType(PartnerType.CUSTOMER).active(true).createdAt(now).updatedAt(now).build();
        partnerMapper.insert(partner);
        partnerId = partner.getId();

        CompanyInfo companyInfo = CompanyInfo.builder().companyName("진보상사").createdAt(now).build();
        companyInfoMapper.insert(companyInfo);
        companyInfoId = companyInfo.getId();

        CompanyUser user = CompanyUser.builder()
                .email("concurrency-test-" + System.nanoTime() + "@myerp.com")
                .password("x").name("동시성테스터").role(UserRole.OWNER)
                .active(true).createdAt(now).updatedAt(now).build();
        companyUserMapper.insert(user);
        userId = user.getId();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM sale_item WHERE sale_id IN (SELECT id FROM sale WHERE partner_id = ?)", partnerId);
        if (itemSpecId != null) {
            jdbcTemplate.update("DELETE FROM stock_history WHERE item_spec_id = ?", itemSpecId);
        }
        jdbcTemplate.update("DELETE FROM sale WHERE partner_id = ?", partnerId);
        if (itemSpecId != null) {
            jdbcTemplate.update("DELETE FROM item_spec WHERE id = ?", itemSpecId);
        }
        if (itemId != null) {
            jdbcTemplate.update("DELETE FROM item WHERE id = ?", itemId);
        }
        if (categorySubId != null) {
            jdbcTemplate.update("DELETE FROM category_sub WHERE id = ?", categorySubId);
        }
        if (categoryMainId != null) {
            jdbcTemplate.update("DELETE FROM category_main WHERE id = ?", categoryMainId);
        }
        jdbcTemplate.update("DELETE FROM partner WHERE id = ?", partnerId);
        jdbcTemplate.update("DELETE FROM company_info WHERE id = ?", companyInfoId);
        jdbcTemplate.update("DELETE FROM company_user WHERE id = ?", userId);
    }

    private Long createItemSpecFixture(int initialStock) {
        LocalDateTime now = LocalDateTime.now();
        CategoryMain main = CategoryMain.builder().name("동시성테스트-" + System.nanoTime()).displayOrder(1).active(true).build();
        categoryMainMapper.insert(main);
        categoryMainId = main.getId();

        CategorySub sub = CategorySub.builder().categoryMainId(main.getId()).name("동시성서브").displayOrder(1).active(true).build();
        categorySubMapper.insert(sub);
        categorySubId = sub.getId();

        Item item = Item.builder().categorySubId(sub.getId()).name("동시성테스트품목").active(true).createdAt(now).updatedAt(now).build();
        itemMapper.insert(item);
        itemId = item.getId();

        ItemSpec spec = ItemSpec.builder().itemId(item.getId()).specName("표준").unit("EA")
                .costPrice(new BigDecimal("1000")).salePrice(new BigDecimal("2000"))
                .currentStock(initialStock).safetyStock(0).active(true).version(0)
                .createdAt(now).updatedAt(now).build();
        itemSpecMapper.insert(spec);
        itemSpecId = spec.getId();

        return itemSpecId;
    }

    private Sale newSaleRequest() {
        return Sale.builder().partnerId(partnerId).companyInfoId(companyInfoId)
                .saleDate(LocalDate.now()).memo("동시성 테스트").build();
    }

    private SaleItem newSaleItem(Long itemSpecId, int quantity) {
        return SaleItem.builder().itemSpecId(itemSpecId).quantity(quantity).unitPrice(new BigDecimal("2000")).build();
    }

    @Test
    void pessimisticLock_concurrentSales_neverOversell() throws InterruptedException {
        int initialStock = 100;
        int quantityPerSale = 10;
        int threadCount = 15;
        Long specId = createItemSpecFixture(initialStock);

        List<Callable<Sale>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> pessimisticLockSaleService.register(
                    newSaleRequest(), List.of(newSaleItem(specId, quantityPerSale)), userId));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Sale>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        int successCount = 0;
        int insufficientStockCount = 0;
        for (Future<Sale> future : futures) {
            try {
                Sale sale = future.get();
                successCount++;
                saleIds.add(sale.getId());
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(InsufficientStockException.class);
                insufficientStockCount++;
            }
        }

        assertThat(successCount).isEqualTo(initialStock / quantityPerSale);
        assertThat(insufficientStockCount).isEqualTo(threadCount - successCount);

        ItemSpec finalSpec = itemSpecMapper.findById(specId).orElseThrow();
        assertThat(finalSpec.getCurrentStock()).isEqualTo(initialStock - successCount * quantityPerSale);
        assertThat(finalSpec.getCurrentStock()).isZero();
    }

    @Test
    void optimisticLock_concurrentSales_neverOversellEvenUnderConflictRetries() throws InterruptedException {
        int initialStock = 100;
        int quantityPerSale = 10;
        int threadCount = 15;
        Long specId = createItemSpecFixture(initialStock);

        List<Callable<Sale>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> optimisticLockSaleService.register(
                    newSaleRequest(), List.of(newSaleItem(specId, quantityPerSale)), userId));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Sale>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        int successCount = 0;
        for (Future<Sale> future : futures) {
            try {
                Sale sale = future.get();
                successCount++;
                saleIds.add(sale.getId());
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOfAny(InsufficientStockException.class, OptimisticLockConflictException.class);
            }
        }

        assertThat(successCount).isLessThanOrEqualTo(initialStock / quantityPerSale);

        ItemSpec finalSpec = itemSpecMapper.findById(specId).orElseThrow();
        assertThat(finalSpec.getCurrentStock()).isEqualTo(initialStock - successCount * quantityPerSale);
        assertThat(finalSpec.getCurrentStock()).isGreaterThanOrEqualTo(0);
    }
}
