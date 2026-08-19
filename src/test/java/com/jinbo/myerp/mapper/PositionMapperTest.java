package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Position;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class PositionMapperTest {

    @Autowired
    private PositionMapper positionMapper;

    private Position newPosition(String name, String allowance) {
        return Position.builder().name(name).allowance(new BigDecimal(allowance)).active(true).build();
    }

    @Test
    void insertAndFindById() {
        Position position = newPosition("팀장", "200000");

        positionMapper.insert(position);

        assertThat(position.getId()).isNotNull();
        Optional<Position> found = positionMapper.findById(position.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("팀장");
        assertThat(found.get().getAllowance()).isEqualByComparingTo("200000");
        // boolean 컬럼(is_active)은 명시적 resultMap이 없으면 매핑되지 않는다(계층 설계 원칙 1).
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findAll_returnsPagedResult() {
        positionMapper.insert(newPosition("팀장", "200000"));
        positionMapper.insert(newPosition("사원", "0"));
        positionMapper.insert(newPosition("대리", "50000"));

        List<Position> page1 = positionMapper.findAll(0, 2);
        int total = positionMapper.countAll();

        assertThat(page1).hasSize(2);
        assertThat(total).isEqualTo(3);
    }

    @Test
    void update_changesNameAllowanceAndActive() {
        Position position = newPosition("변경전", "0");
        positionMapper.insert(position);

        position.setName("변경후");
        position.setAllowance(new BigDecimal("100000"));
        position.setActive(false);
        positionMapper.update(position);

        Position updated = positionMapper.findById(position.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("변경후");
        assertThat(updated.getAllowance()).isEqualByComparingTo("100000");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void findByName_whenExists_returnsIt() {
        positionMapper.insert(newPosition("팀장", "200000"));

        Optional<Position> found = positionMapper.findByName("팀장");

        assertThat(found).isPresent();
    }

    @Test
    void findByName_whenNotExists_returnsEmpty() {
        assertThat(positionMapper.findByName("없는직책")).isEmpty();
    }
}
