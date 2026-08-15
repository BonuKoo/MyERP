package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.domain.SaleItem;
import com.jinbo.myerp.domain.SaleStatus;
import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;
import com.jinbo.myerp.exception.CompanyInfoNotFoundException;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.exception.SaleNotFoundException;
import com.jinbo.myerp.mapper.CompanyInfoMapper;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import com.jinbo.myerp.mapper.PartnerMapper;
import com.jinbo.myerp.mapper.SaleItemMapper;
import com.jinbo.myerp.mapper.SaleMapper;
import com.jinbo.myerp.mapper.StockHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service("pessimisticLockSaleService")
@RequiredArgsConstructor
public class PessimisticLockSaleService implements SaleService {

    private static final AtomicLong SALE_NO_SEQUENCE = new AtomicLong();

    private final SaleMapper saleMapper;
    private final SaleItemMapper saleItemMapper;
    private final PartnerMapper partnerMapper;
    private final CompanyInfoMapper companyInfoMapper;
    private final ItemSpecMapper itemSpecMapper;
    private final StockHistoryMapper stockHistoryMapper;

    @Override
    @Transactional
    public Sale register(Sale sale, List<SaleItem> items, Long userId) {
        partnerMapper.findById(sale.getPartnerId())
                .orElseThrow(() -> new PartnerNotFoundException(sale.getPartnerId()));
        companyInfoMapper.findById(sale.getCompanyInfoId())
                .orElseThrow(() -> new CompanyInfoNotFoundException(sale.getCompanyInfoId()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ItemSpec> specs = new ArrayList<>();
        for (SaleItem item : items) {
            ItemSpec spec = itemSpecMapper.findByIdForUpdate(item.getItemSpecId())
                    .orElseThrow(() -> new ItemSpecNotFoundException(item.getItemSpecId()));
            if (spec.getCurrentStock() < item.getQuantity()) {
                throw new InsufficientStockException(spec.getId(), spec.getCurrentStock(), -item.getQuantity());
            }
            specs.add(spec);
            item.setAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getAmount());
        }

        sale.setSaleNo("SO" + System.currentTimeMillis() + "-" + SALE_NO_SEQUENCE.incrementAndGet());
        sale.setStatus(SaleStatus.CONFIRMED);
        sale.setTotalAmount(totalAmount);
        sale.setCreatedBy(userId);
        sale.setCreatedAt(LocalDateTime.now());
        saleMapper.insert(sale);

        for (int i = 0; i < items.size(); i++) {
            SaleItem item = items.get(i);
            item.setSaleId(sale.getId());
            saleItemMapper.insert(item);
            decreaseStock(specs.get(i), item.getQuantity(), sale.getId(), userId);
        }

        return sale;
    }

    private void decreaseStock(ItemSpec spec, int quantity, Long saleId, Long userId) {
        Long itemSpecId = spec.getId();
        int beforeStock = spec.getCurrentStock();
        int afterStock = beforeStock - quantity;
        spec.setCurrentStock(afterStock);
        spec.setVersion(spec.getVersion() + 1);
        spec.setUpdatedAt(LocalDateTime.now());
        itemSpecMapper.update(spec);

        StockHistory history = StockHistory.builder()
                .itemSpecId(itemSpecId)
                .changeType(StockChangeType.SALE_OUT)
                .quantity(-quantity)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .relatedDocumentType("SALE")
                .relatedDocumentId(saleId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        stockHistoryMapper.insert(history);
    }

    @Override
    public Sale findById(Long id) {
        return saleMapper.findById(id)
                .orElseThrow(() -> new SaleNotFoundException(id));
    }

    @Override
    public List<SaleItem> findItemsBySaleId(Long saleId) {
        return saleItemMapper.findBySaleId(saleId);
    }

    @Override
    public PageResult<Sale> findAll(int page, int size) {
        int offset = page * size;
        List<Sale> content = saleMapper.findAll(offset, size);
        long totalCount = saleMapper.countAll();
        return new PageResult<>(content, totalCount, page, size);
    }

    @Override
    @Transactional
    public Sale cancel(Long id, Long userId) {
        Sale sale = findById(id);
        if (sale.getStatus() == SaleStatus.CANCELED) {
            throw new InvalidStatusTransitionException("이미 취소된 매출 전표입니다: id=" + id);
        }

        List<SaleItem> items = saleItemMapper.findBySaleId(id);
        for (SaleItem item : items) {
            reverseStock(item.getItemSpecId(), item.getQuantity(), id, userId);
        }

        sale.setStatus(SaleStatus.CANCELED);
        sale.setCanceledAt(LocalDateTime.now());
        saleMapper.updateStatus(sale);

        return sale;
    }

    private void reverseStock(Long itemSpecId, int quantity, Long saleId, Long userId) {
        ItemSpec spec = itemSpecMapper.findByIdForUpdate(itemSpecId)
                .orElseThrow(() -> new ItemSpecNotFoundException(itemSpecId));
        int beforeStock = spec.getCurrentStock();
        int afterStock = beforeStock + quantity;
        spec.setCurrentStock(afterStock);
        spec.setVersion(spec.getVersion() + 1);
        spec.setUpdatedAt(LocalDateTime.now());
        itemSpecMapper.update(spec);

        StockHistory history = StockHistory.builder()
                .itemSpecId(itemSpecId)
                .changeType(StockChangeType.ADJUST)
                .quantity(quantity)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .relatedDocumentType("SALE")
                .relatedDocumentId(saleId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        stockHistoryMapper.insert(history);
    }
}
