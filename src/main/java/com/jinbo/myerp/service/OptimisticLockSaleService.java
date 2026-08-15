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
import com.jinbo.myerp.exception.OptimisticLockConflictException;
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
import java.util.List;

@Service("optimisticLockSaleService")
@RequiredArgsConstructor
public class OptimisticLockSaleService implements SaleService {

    private static final int MAX_RETRIES = 3;

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
        for (SaleItem item : items) {
            ItemSpec spec = itemSpecMapper.findById(item.getItemSpecId())
                    .orElseThrow(() -> new ItemSpecNotFoundException(item.getItemSpecId()));
            if (spec.getCurrentStock() < item.getQuantity()) {
                throw new InsufficientStockException(spec.getId(), spec.getCurrentStock(), -item.getQuantity());
            }
            item.setAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getAmount());
        }

        sale.setSaleNo("SO" + System.currentTimeMillis());
        sale.setStatus(SaleStatus.CONFIRMED);
        sale.setTotalAmount(totalAmount);
        sale.setCreatedBy(userId);
        sale.setCreatedAt(LocalDateTime.now());
        saleMapper.insert(sale);

        for (SaleItem item : items) {
            item.setSaleId(sale.getId());
            saleItemMapper.insert(item);
            applyStockChange(item.getItemSpecId(), -item.getQuantity(), StockChangeType.SALE_OUT, sale.getId(), userId);
        }

        return sale;
    }

    private void applyStockChange(Long itemSpecId, int quantityDelta, StockChangeType changeType, Long saleId, Long userId) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            ItemSpec spec = itemSpecMapper.findById(itemSpecId)
                    .orElseThrow(() -> new ItemSpecNotFoundException(itemSpecId));
            int beforeStock = spec.getCurrentStock();
            int afterStock = beforeStock + quantityDelta;
            if (afterStock < 0) {
                throw new InsufficientStockException(itemSpecId, beforeStock, quantityDelta);
            }

            int affected = itemSpecMapper.updateStockOptimistic(itemSpecId, afterStock, spec.getVersion(), LocalDateTime.now());
            if (affected == 1) {
                StockHistory history = StockHistory.builder()
                        .itemSpecId(itemSpecId)
                        .changeType(changeType)
                        .quantity(quantityDelta)
                        .beforeStock(beforeStock)
                        .afterStock(afterStock)
                        .relatedDocumentType("SALE")
                        .relatedDocumentId(saleId)
                        .createdBy(userId)
                        .createdAt(LocalDateTime.now())
                        .build();
                stockHistoryMapper.insert(history);
                return;
            }
        }
        throw new OptimisticLockConflictException(itemSpecId);
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
            applyStockChange(item.getItemSpecId(), item.getQuantity(), StockChangeType.ADJUST, id, userId);
        }

        sale.setStatus(SaleStatus.CANCELED);
        sale.setCanceledAt(LocalDateTime.now());
        saleMapper.updateStatus(sale);

        return sale;
    }
}
