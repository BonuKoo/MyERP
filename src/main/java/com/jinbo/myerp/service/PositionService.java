package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Position;
import com.jinbo.myerp.exception.PositionAlreadyExistsException;
import com.jinbo.myerp.exception.PositionNotFoundException;
import com.jinbo.myerp.mapper.PositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionMapper positionMapper;

    @Transactional
    public Position register(Position position) {
        verifyNameNotTaken(position.getName(), null);
        position.setActive(true);
        positionMapper.insert(position);
        return position;
    }

    public Position findById(Long id) {
        return positionMapper.findById(id).orElseThrow(() -> new PositionNotFoundException(id));
    }

    public PageResult<Position> findAll(int page, int size) {
        int offset = page * size;
        return new PageResult<>(positionMapper.findAll(offset, size), positionMapper.countAll(), page, size);
    }

    @Transactional
    public Position update(Long id, String newName, BigDecimal newAllowance) {
        Position position = findById(id);
        verifyNameNotTaken(newName, id);
        position.setName(newName);
        position.setAllowance(newAllowance);
        positionMapper.update(position);
        return position;
    }

    @Transactional
    public void deactivate(Long id) {
        Position position = findById(id);
        position.setActive(false);
        positionMapper.update(position);
    }

    private void verifyNameNotTaken(String name, Long excludingId) {
        positionMapper.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(excludingId)) {
                throw new PositionAlreadyExistsException(name);
            }
        });
    }
}
