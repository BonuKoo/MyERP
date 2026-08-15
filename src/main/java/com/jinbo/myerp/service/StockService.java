package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import com.jinbo.myerp.mapper.StockHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ItemSpecMapper itemSpecMapper;
    private final StockHistoryMapper stockHistoryMapper;

    @Transactional
    public ItemSpec adjustStock(Long itemSpecId, int quantityDelta, Long userId) {
        ItemSpec itemSpec = itemSpecMapper.findById(itemSpecId)
                .orElseThrow(() -> new ItemSpecNotFoundException(itemSpecId));

        int beforeStock = itemSpec.getCurrentStock();
        int afterStock = beforeStock + quantityDelta;
        if (afterStock < 0) {
            throw new InsufficientStockException(itemSpecId, beforeStock, quantityDelta);
        }

        itemSpec.setCurrentStock(afterStock);
        itemSpec.setVersion(itemSpec.getVersion() + 1);
        itemSpec.setUpdatedAt(LocalDateTime.now());
        itemSpecMapper.update(itemSpec);

        StockHistory history = StockHistory.builder()
                .itemSpecId(itemSpecId)
                .changeType(StockChangeType.ADJUST)
                .quantity(quantityDelta)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .relatedDocumentType("MANUAL")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        stockHistoryMapper.insert(history);

        return itemSpec;
    }

    public PageResult<StockHistory> findHistoryByItemSpecId(Long itemSpecId, int page, int size) {
        int offset = page * size;
        List<StockHistory> content = stockHistoryMapper.findByItemSpecId(itemSpecId, offset, size);
        long totalCount = stockHistoryMapper.countByItemSpecId(itemSpecId);
        return new PageResult<>(content, totalCount, page, size);
    }
}
