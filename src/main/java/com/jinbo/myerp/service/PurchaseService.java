package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.domain.Purchase;
import com.jinbo.myerp.domain.PurchaseItem;
import com.jinbo.myerp.domain.PurchaseStatus;
import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;
import com.jinbo.myerp.exception.CompanyInfoNotFoundException;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.exception.PurchaseNotFoundException;
import com.jinbo.myerp.mapper.CompanyInfoMapper;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import com.jinbo.myerp.mapper.PartnerMapper;
import com.jinbo.myerp.mapper.PurchaseItemMapper;
import com.jinbo.myerp.mapper.PurchaseMapper;
import com.jinbo.myerp.mapper.StockHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseMapper purchaseMapper;
    private final PurchaseItemMapper purchaseItemMapper;
    private final PartnerMapper partnerMapper;
    private final CompanyInfoMapper companyInfoMapper;
    private final ItemSpecMapper itemSpecMapper;
    private final StockHistoryMapper stockHistoryMapper;
    private final LedgerService ledgerService;

    @Transactional
    public Purchase register(Purchase purchase, List<PurchaseItem> items, Long userId) {
        partnerMapper.findById(purchase.getPartnerId())
                .orElseThrow(() -> new PartnerNotFoundException(purchase.getPartnerId()));
        companyInfoMapper.findById(purchase.getCompanyInfoId())
                .orElseThrow(() -> new CompanyInfoNotFoundException(purchase.getCompanyInfoId()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ItemSpec> specs = new ArrayList<>();
        for (PurchaseItem item : items) {
            ItemSpec spec = itemSpecMapper.findById(item.getItemSpecId())
                    .orElseThrow(() -> new ItemSpecNotFoundException(item.getItemSpecId()));
            specs.add(spec);
            item.setAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getAmount());
        }

        // partner 잔액 조정을 purchase insert보다 먼저 실행해 partner 행의 배타락을 선점해야
        // 한다. 자세한 이유는 OptimisticLockSaleService.register()의 동일 주석 참고.
        BigDecimal balanceAfter = ledgerService.adjustPayableBalance(purchase.getPartnerId(), totalAmount);

        purchase.setPurchaseNo("PO" + System.currentTimeMillis());
        purchase.setStatus(PurchaseStatus.CONFIRMED);
        purchase.setTotalAmount(totalAmount);
        purchase.setCreatedBy(userId);
        purchase.setCreatedAt(LocalDateTime.now());
        purchaseMapper.insert(purchase);

        for (int i = 0; i < items.size(); i++) {
            PurchaseItem item = items.get(i);
            item.setPurchaseId(purchase.getId());
            purchaseItemMapper.insert(item);
            increaseStock(specs.get(i), item.getQuantity(), purchase.getId(), userId);
        }

        ledgerService.recordEntry(purchase.getPartnerId(), LedgerType.PAYABLE, LedgerChangeType.PURCHASE_CONFIRMED,
                totalAmount, balanceAfter, "PURCHASE", purchase.getId(), userId);

        return purchase;
    }

    private void increaseStock(ItemSpec spec, int quantity, Long purchaseId, Long userId) {
        Long itemSpecId = spec.getId();
        int beforeStock = spec.getCurrentStock();
        int afterStock = beforeStock + quantity;
        spec.setCurrentStock(afterStock);
        spec.setVersion(spec.getVersion() + 1);
        spec.setUpdatedAt(LocalDateTime.now());
        itemSpecMapper.update(spec);

        StockHistory history = StockHistory.builder()
                .itemSpecId(itemSpecId)
                .changeType(StockChangeType.PURCHASE_IN)
                .quantity(quantity)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .relatedDocumentType("PURCHASE")
                .relatedDocumentId(purchaseId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        stockHistoryMapper.insert(history);
    }

    public Purchase findById(Long id) {
        return purchaseMapper.findById(id)
                .orElseThrow(() -> new PurchaseNotFoundException(id));
    }

    public List<PurchaseItem> findItemsByPurchaseId(Long purchaseId) {
        return purchaseItemMapper.findByPurchaseId(purchaseId);
    }

    public PageResult<Purchase> findAll(int page, int size) {
        int offset = page * size;
        List<Purchase> content = purchaseMapper.findAll(offset, size);
        long totalCount = purchaseMapper.countAll();
        return new PageResult<>(content, totalCount, page, size);
    }

    @Transactional
    public Purchase cancel(Long id, Long userId) {
        Purchase purchase = findById(id);
        if (purchase.getStatus() == PurchaseStatus.CANCELED) {
            throw new InvalidStatusTransitionException("이미 취소된 매입 전표입니다: id=" + id);
        }

        List<PurchaseItem> items = purchaseItemMapper.findByPurchaseId(id);
        for (PurchaseItem item : items) {
            reverseStock(item.getItemSpecId(), item.getQuantity(), id, userId);
        }

        purchase.setStatus(PurchaseStatus.CANCELED);
        purchase.setCanceledAt(LocalDateTime.now());
        purchaseMapper.updateStatus(purchase);

        ledgerService.recordPayableChange(purchase.getPartnerId(), LedgerChangeType.PURCHASE_CANCELED,
                purchase.getTotalAmount().negate(), "PURCHASE", purchase.getId(), userId);

        return purchase;
    }

    private void reverseStock(Long itemSpecId, int quantity, Long purchaseId, Long userId) {
        ItemSpec spec = itemSpecMapper.findById(itemSpecId)
                .orElseThrow(() -> new ItemSpecNotFoundException(itemSpecId));
        int beforeStock = spec.getCurrentStock();
        int afterStock = beforeStock - quantity;
        if (afterStock < 0) {
            throw new InsufficientStockException(itemSpecId, beforeStock, -quantity);
        }
        spec.setCurrentStock(afterStock);
        spec.setVersion(spec.getVersion() + 1);
        spec.setUpdatedAt(LocalDateTime.now());
        itemSpecMapper.update(spec);

        StockHistory history = StockHistory.builder()
                .itemSpecId(itemSpecId)
                .changeType(StockChangeType.ADJUST)
                .quantity(-quantity)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .relatedDocumentType("PURCHASE")
                .relatedDocumentId(purchaseId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        stockHistoryMapper.insert(history);
    }
}
