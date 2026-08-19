package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.exception.DepartmentAlreadyExistsException;
import com.jinbo.myerp.exception.DepartmentNotFoundException;
import com.jinbo.myerp.mapper.DepartmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void register_setsActiveTrue() {
        given(departmentMapper.findByName("인사팀")).willReturn(Optional.empty());
        Department dept = Department.builder().name("인사팀").build();

        Department result = departmentService.register(dept);

        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();
        verify(departmentMapper).insert(dept);
    }

    @Test
    void register_duplicateName_throws() {
        given(departmentMapper.findByName("인사팀"))
                .willReturn(Optional.of(Department.builder().id(1L).name("인사팀").build()));

        assertThatThrownBy(() -> departmentService.register(Department.builder().name("인사팀").build()))
                .isInstanceOf(DepartmentAlreadyExistsException.class);
    }

    @Test
    void findById_notFound_throws() {
        given(departmentMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.findById(1L))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void findAll_returnsPageResult() {
        List<Department> depts = List.of(Department.builder().id(1L).name("인사팀").build());
        given(departmentMapper.findAll(0, 10)).willReturn(depts);
        given(departmentMapper.countAll()).willReturn(1);

        PageResult<Department> result = departmentService.findAll(0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void rename_appliesChange() {
        Department existing = Department.builder().id(1L).name("변경전").active(true).build();
        given(departmentMapper.findById(1L)).willReturn(Optional.of(existing));
        given(departmentMapper.findByName("변경후")).willReturn(Optional.empty());

        Department result = departmentService.rename(1L, "변경후");

        assertThat(result.getName()).isEqualTo("변경후");
        verify(departmentMapper).update(existing);
    }

    @Test
    void rename_toDuplicateName_throws() {
        Department existing = Department.builder().id(1L).name("변경전").active(true).build();
        given(departmentMapper.findById(1L)).willReturn(Optional.of(existing));
        given(departmentMapper.findByName("이미있음"))
                .willReturn(Optional.of(Department.builder().id(2L).name("이미있음").build()));

        assertThatThrownBy(() -> departmentService.rename(1L, "이미있음"))
                .isInstanceOf(DepartmentAlreadyExistsException.class);
    }

    /** 자기 자신과 같은 이름으로 "변경"하는 건 중복이 아니다 - id가 같으면 통과해야 한다. */
    @Test
    void rename_toSameNameAsSelf_doesNotThrow() {
        Department existing = Department.builder().id(1L).name("인사팀").active(true).build();
        given(departmentMapper.findById(1L)).willReturn(Optional.of(existing));
        given(departmentMapper.findByName("인사팀")).willReturn(Optional.of(existing));

        Department result = departmentService.rename(1L, "인사팀");

        assertThat(result.getName()).isEqualTo("인사팀");
    }

    @Test
    void deactivate_setsActiveFalse() {
        Department dept = Department.builder().id(1L).name("인사팀").active(true).build();
        given(departmentMapper.findById(1L)).willReturn(Optional.of(dept));

        departmentService.deactivate(1L);

        assertThat(dept.isActive()).isFalse();
        verify(departmentMapper).update(dept);
    }
}
