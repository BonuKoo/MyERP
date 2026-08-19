package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.domain.Position;
import com.jinbo.myerp.exception.CompanyUserNotFoundException;
import com.jinbo.myerp.exception.DepartmentNotFoundException;
import com.jinbo.myerp.exception.EmployeeAlreadyLinkedException;
import com.jinbo.myerp.exception.EmployeeNotFoundException;
import com.jinbo.myerp.exception.PositionNotFoundException;
import com.jinbo.myerp.mapper.CompanyUserMapper;
import com.jinbo.myerp.mapper.DepartmentMapper;
import com.jinbo.myerp.mapper.EmployeeMapper;
import com.jinbo.myerp.mapper.PositionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private PositionMapper positionMapper;

    @Mock
    private CompanyUserMapper companyUserMapper;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee newEmployee() {
        return Employee.builder().departmentId(1L).positionId(1L).name("김철수").hireDate(LocalDate.of(2026, 1, 1)).build();
    }

    @Test
    void register_setsActiveTrue() {
        given(departmentMapper.findById(1L)).willReturn(Optional.of(Department.builder().id(1L).build()));
        given(positionMapper.findById(1L)).willReturn(Optional.of(Position.builder().id(1L).build()));

        Employee result = employeeService.register(newEmployee());

        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();
        verify(employeeMapper).insert(result);
    }

    @Test
    void register_departmentNotFound_throws() {
        given(departmentMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.register(newEmployee()))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void register_positionNotFound_throws() {
        given(departmentMapper.findById(1L)).willReturn(Optional.of(Department.builder().id(1L).build()));
        given(positionMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.register(newEmployee()))
                .isInstanceOf(PositionNotFoundException.class);
    }

    @Test
    void register_withCompanyUserId_verifiesExistsAndNotLinked() {
        given(departmentMapper.findById(1L)).willReturn(Optional.of(Department.builder().id(1L).build()));
        given(positionMapper.findById(1L)).willReturn(Optional.of(Position.builder().id(1L).build()));
        given(companyUserMapper.findById(9L)).willReturn(Optional.of(CompanyUser.builder().id(9L).build()));
        given(employeeMapper.findByCompanyUserId(9L)).willReturn(Optional.empty());
        Employee employee = newEmployee();
        employee.setCompanyUserId(9L);

        Employee result = employeeService.register(employee);

        assertThat(result.getCompanyUserId()).isEqualTo(9L);
    }

    @Test
    void register_companyUserNotFound_throws() {
        given(departmentMapper.findById(1L)).willReturn(Optional.of(Department.builder().id(1L).build()));
        given(positionMapper.findById(1L)).willReturn(Optional.of(Position.builder().id(1L).build()));
        given(companyUserMapper.findById(9L)).willReturn(Optional.empty());
        Employee employee = newEmployee();
        employee.setCompanyUserId(9L);

        assertThatThrownBy(() -> employeeService.register(employee))
                .isInstanceOf(CompanyUserNotFoundException.class);
    }

    @Test
    void register_companyUserAlreadyLinked_throws() {
        given(departmentMapper.findById(1L)).willReturn(Optional.of(Department.builder().id(1L).build()));
        given(positionMapper.findById(1L)).willReturn(Optional.of(Position.builder().id(1L).build()));
        given(companyUserMapper.findById(9L)).willReturn(Optional.of(CompanyUser.builder().id(9L).build()));
        given(employeeMapper.findByCompanyUserId(9L)).willReturn(Optional.of(Employee.builder().id(2L).companyUserId(9L).build()));
        Employee employee = newEmployee();
        employee.setCompanyUserId(9L);

        assertThatThrownBy(() -> employeeService.register(employee))
                .isInstanceOf(EmployeeAlreadyLinkedException.class);
    }

    @Test
    void findById_notFound_throws() {
        given(employeeMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById(1L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void findAll_returnsPageResult() {
        List<Employee> employees = List.of(newEmployee());
        given(employeeMapper.findAll(0, 10, null, null, null)).willReturn(employees);
        given(employeeMapper.countAll(null, null, null)).willReturn(1);

        PageResult<Employee> result = employeeService.findAll(0, 10, null, null, null);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void resign_setsResignationDateAndInactive() {
        Employee employee = Employee.builder().id(1L).active(true).build();
        given(employeeMapper.findById(1L)).willReturn(Optional.of(employee));

        employeeService.resign(1L, LocalDate.of(2026, 6, 30));

        assertThat(employee.isActive()).isFalse();
        assertThat(employee.getResignationDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        verify(employeeMapper).update(employee);
    }

    @Test
    void findByCurrentUser_returnsLinkedEmployee() {
        Employee linked = Employee.builder().id(1L).companyUserId(9L).build();
        given(employeeMapper.findByCompanyUserId(9L)).willReturn(Optional.of(linked));

        Employee result = employeeService.findByCompanyUserId(9L);

        assertThat(result).isEqualTo(linked);
    }

    @Test
    void findByCurrentUser_notLinked_throws() {
        given(employeeMapper.findByCompanyUserId(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findByCompanyUserId(9L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
