package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.mapper.PartnerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerMapper partnerMapper;

    @Transactional
    public Partner register(Partner partner) {
        LocalDateTime now = LocalDateTime.now();
        partner.setActive(true);
        partner.setCreatedAt(now);
        partner.setUpdatedAt(now);
        partnerMapper.insert(partner);
        return partner;
    }

    public Partner findById(Long id) {
        return partnerMapper.findById(id)
                .orElseThrow(() -> new PartnerNotFoundException(id));
    }

    public PageResult<Partner> findAll(int page, int size) {
        int offset = page * size;
        List<Partner> content = partnerMapper.findAll(offset, size);
        long totalCount = partnerMapper.countAll();
        return new PageResult<>(content, totalCount, page, size);
    }

    @Transactional
    public Partner update(Long id, Partner changes) {
        Partner partner = findById(id);
        partner.setName(changes.getName());
        partner.setBusinessNumber(changes.getBusinessNumber());
        partner.setPartnerType(changes.getPartnerType());
        partner.setContactName(changes.getContactName());
        partner.setContactPhone(changes.getContactPhone());
        partner.setAddress(changes.getAddress());
        partner.setUpdatedAt(LocalDateTime.now());
        partnerMapper.update(partner);
        return partner;
    }

    @Transactional
    public void deactivate(Long id) {
        Partner partner = findById(id);
        partner.setActive(false);
        partnerMapper.update(partner);
    }
}
