package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.mapper.ItemMapper;
import com.jinbo.myerp.mapper.ItemSpecMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemSpecService {

    private final ItemSpecMapper itemSpecMapper;
    private final ItemMapper itemMapper;

    @Transactional
    public ItemSpec register(ItemSpec itemSpec) {
        itemMapper.findById(itemSpec.getItemId())
                .orElseThrow(() -> new ItemNotFoundException(itemSpec.getItemId()));

        LocalDateTime now = LocalDateTime.now();
        itemSpec.setActive(true);
        itemSpec.setCurrentStock(0);
        itemSpec.setVersion(0);
        itemSpec.setCreatedAt(now);
        itemSpec.setUpdatedAt(now);
        itemSpecMapper.insert(itemSpec);
        return itemSpec;
    }

    public List<ItemSpec> findByItemId(Long itemId) {
        return itemSpecMapper.findByItemId(itemId);
    }

    public ItemSpec findById(Long id) {
        return itemSpecMapper.findById(id)
                .orElseThrow(() -> new ItemSpecNotFoundException(id));
    }

    @Transactional
    public ItemSpec update(Long id, ItemSpec changes) {
        ItemSpec itemSpec = findById(id);
        itemSpec.setSpecName(changes.getSpecName());
        itemSpec.setUnit(changes.getUnit());
        itemSpec.setCostPrice(changes.getCostPrice());
        itemSpec.setSalePrice(changes.getSalePrice());
        itemSpec.setSafetyStock(changes.getSafetyStock());
        itemSpec.setUpdatedAt(LocalDateTime.now());
        itemSpecMapper.update(itemSpec);
        return itemSpec;
    }
}
