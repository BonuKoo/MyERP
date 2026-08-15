package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.exception.CertificationAlreadyExistsException;
import com.jinbo.myerp.mapper.CertificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationMapper certificationMapper;

    @Transactional
    public Certification register(String name) {
        certificationMapper.findByName(name).ifPresent(c -> {
            throw new CertificationAlreadyExistsException(name);
        });

        Certification certification = Certification.builder().name(name).build();
        certificationMapper.insert(certification);
        return certification;
    }

    public List<Certification> findAll() {
        return certificationMapper.findAll();
    }
}
