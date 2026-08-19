package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.exception.DepartmentAlreadyExistsException;
import com.jinbo.myerp.exception.DepartmentNotFoundException;
import com.jinbo.myerp.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Transactional
    public Department register(Department department) {
        verifyNameNotTaken(department.getName(), null);

        department.setActive(true);
        department.setCreatedAt(LocalDateTime.now());
        departmentMapper.insert(department);
        return department;
    }

    public Department findById(Long id) {
        return departmentMapper.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
    }

    public PageResult<Department> findAll(int page, int size) {
        int offset = page * size;
        return new PageResult<>(departmentMapper.findAll(offset, size), departmentMapper.countAll(), page, size);
    }

    @Transactional
    public Department rename(Long id, String newName) {
        Department department = findById(id);
        verifyNameNotTaken(newName, id);

        department.setName(newName);
        departmentMapper.update(department);
        return department;
    }

    @Transactional
    public void deactivate(Long id) {
        Department department = findById(id);
        department.setActive(false);
        departmentMapper.update(department);
    }

    /** DB의 UNIQUE(name) 제약에 기대지 않고 먼저 확인한다 - 그러지 않으면 CategoryMain처럼
     * 중복 등록이 500으로 새 나가는 결함을 반복하게 된다. */
    private void verifyNameNotTaken(String name, Long excludingId) {
        departmentMapper.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(excludingId)) {
                throw new DepartmentAlreadyExistsException(name);
            }
        });
    }
}
