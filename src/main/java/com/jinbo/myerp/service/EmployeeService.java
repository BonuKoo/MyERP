package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.exception.CompanyUserNotFoundException;
import com.jinbo.myerp.exception.DepartmentNotFoundException;
import com.jinbo.myerp.exception.EmployeeAlreadyLinkedException;
import com.jinbo.myerp.exception.EmployeeNotFoundException;
import com.jinbo.myerp.exception.PositionNotFoundException;
import com.jinbo.myerp.mapper.CompanyUserMapper;
import com.jinbo.myerp.mapper.DepartmentMapper;
import com.jinbo.myerp.mapper.EmployeeMapper;
import com.jinbo.myerp.mapper.PositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final CompanyUserMapper companyUserMapper;

    @Transactional
    public Employee register(Employee employee) {
        verifyDepartmentExists(employee.getDepartmentId());
        verifyPositionExists(employee.getPositionId());
        if (employee.getCompanyUserId() != null) {
            verifyCompanyUserLinkable(employee.getCompanyUserId());
        }
        employee.setActive(true);
        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        employeeMapper.insert(employee);
        return employee;
    }

    public Employee findById(Long id) {
        return employeeMapper.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    public Employee findByCompanyUserId(Long companyUserId) {
        return employeeMapper.findByCompanyUserId(companyUserId)
                .orElseThrow(() -> new EmployeeNotFoundException("연결된 사원 정보가 없습니다: companyUserId=" + companyUserId));
    }

    public PageResult<Employee> findAll(int page, int size, Long departmentId, Long positionId, String name) {
        int offset = page * size;
        return new PageResult<>(
                employeeMapper.findAll(offset, size, departmentId, positionId, name),
                employeeMapper.countAll(departmentId, positionId, name),
                page, size);
    }

    @Transactional
    public Employee update(Long id, Long departmentId, Long positionId, String name, String phone, String email) {
        Employee employee = findById(id);
        verifyDepartmentExists(departmentId);
        verifyPositionExists(positionId);
        employee.setDepartmentId(departmentId);
        employee.setPositionId(positionId);
        employee.setName(name);
        employee.setPhone(phone);
        employee.setEmail(email);
        employee.setUpdatedAt(LocalDateTime.now());
        employeeMapper.update(employee);
        return employee;
    }

    @Transactional
    public void resign(Long id, LocalDate resignationDate) {
        Employee employee = findById(id);
        employee.setResignationDate(resignationDate);
        employee.setActive(false);
        employee.setUpdatedAt(LocalDateTime.now());
        employeeMapper.update(employee);
    }

    private void verifyDepartmentExists(Long departmentId) {
        departmentMapper.findById(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));
    }

    private void verifyPositionExists(Long positionId) {
        positionMapper.findById(positionId).orElseThrow(() -> new PositionNotFoundException(positionId));
    }

    private void verifyCompanyUserLinkable(Long companyUserId) {
        companyUserMapper.findById(companyUserId).orElseThrow(() -> new CompanyUserNotFoundException(companyUserId));
        employeeMapper.findByCompanyUserId(companyUserId).ifPresent(existing -> {
            throw new EmployeeAlreadyLinkedException(companyUserId);
        });
    }
}
