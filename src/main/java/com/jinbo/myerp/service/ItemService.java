package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.exception.CategorySubNotFoundException;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.mapper.CategorySubMapper;
import com.jinbo.myerp.mapper.ItemCertificationMapper;
import com.jinbo.myerp.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final CategorySubMapper categorySubMapper;
    private final ItemCertificationMapper itemCertificationMapper;

    @Transactional
    public Item register(Item item, List<Long> certificationIds) {
        categorySubMapper.findById(item.getCategorySubId())
                .orElseThrow(() -> new CategorySubNotFoundException(item.getCategorySubId()));

        LocalDateTime now = LocalDateTime.now();
        item.setActive(true);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        itemMapper.insert(item);

        for (Long certificationId : certificationIds) {
            itemCertificationMapper.insert(item.getId(), certificationId);
        }

        return item;
    }

    public Item findById(Long id) {
        return itemMapper.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    public PageResult<Item> findAll(int page, int size) {
        int offset = page * size;
        List<Item> content = itemMapper.findAll(offset, size);
        long totalCount = itemMapper.countAll();
        return new PageResult<>(content, totalCount, page, size);
    }

    public List<Certification> findCertifications(Long itemId) {
        return itemCertificationMapper.findCertificationsByItemId(itemId);
    }

    @Transactional
    public Item update(Long id, Item changes, List<Long> certificationIds) {
        Item item = findById(id);
        item.setCategorySubId(changes.getCategorySubId());
        item.setName(changes.getName());
        item.setDescription(changes.getDescription());
        item.setKsStandard(changes.getKsStandard());
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.update(item);

        itemCertificationMapper.deleteByItemId(id);
        for (Long certificationId : certificationIds) {
            itemCertificationMapper.insert(id, certificationId);
        }

        return item;
    }
}
