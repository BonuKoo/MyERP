package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Department;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class DepartmentMapperTest {

    @Autowired
    private DepartmentMapper departmentMapper;

    private Department newDepartment(String name) {
        return Department.builder().name(name).active(true).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void insertAndFindById() {
        Department dept = newDepartment("인사팀");

        departmentMapper.insert(dept);

        assertThat(dept.getId()).isNotNull();
        Optional<Department> found = departmentMapper.findById(dept.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("인사팀");
        // boolean 컬럼(is_active)은 명시적 resultMap이 없으면 매핑되지 않는다(계층 설계 원칙 1).
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findAll_returnsPagedResult() {
        departmentMapper.insert(newDepartment("인사팀"));
        departmentMapper.insert(newDepartment("재무팀"));
        departmentMapper.insert(newDepartment("영업팀"));

        List<Department> page1 = departmentMapper.findAll(0, 2);
        int total = departmentMapper.countAll();

        assertThat(page1).hasSize(2);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void update_changesNameAndActive() {
        Department dept = newDepartment("변경전");
        departmentMapper.insert(dept);

        dept.setName("변경후");
        dept.setActive(false);
        departmentMapper.update(dept);

        Department updated = departmentMapper.findById(dept.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("변경후");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void findByName_whenExists_returnsIt() {
        departmentMapper.insert(newDepartment("인사팀"));

        Optional<Department> found = departmentMapper.findByName("인사팀");

        assertThat(found).isPresent();
    }

    @Test
    void findByName_whenNotExists_returnsEmpty() {
        assertThat(departmentMapper.findByName("없는부서")).isEmpty();
    }
}
