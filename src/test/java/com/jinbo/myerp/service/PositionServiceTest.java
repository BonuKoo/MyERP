package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Position;
import com.jinbo.myerp.exception.PositionAlreadyExistsException;
import com.jinbo.myerp.exception.PositionNotFoundException;
import com.jinbo.myerp.mapper.PositionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionMapper positionMapper;

    @InjectMocks
    private PositionService positionService;

    @Test
    void register_setsActiveTrue() {
        given(positionMapper.findByName("팀장")).willReturn(Optional.empty());
        Position position = Position.builder().name("팀장").allowance(new BigDecimal("200000")).build();

        Position result = positionService.register(position);

        assertThat(result.isActive()).isTrue();
        verify(positionMapper).insert(position);
    }

    @Test
    void register_duplicateName_throws() {
        given(positionMapper.findByName("팀장"))
                .willReturn(Optional.of(Position.builder().id(1L).name("팀장").build()));

        assertThatThrownBy(() -> positionService.register(Position.builder().name("팀장").build()))
                .isInstanceOf(PositionAlreadyExistsException.class);
    }

    @Test
    void findById_notFound_throws() {
        given(positionMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> positionService.findById(1L))
                .isInstanceOf(PositionNotFoundException.class);
    }

    @Test
    void findAll_returnsPageResult() {
        List<Position> positions = List.of(Position.builder().id(1L).name("팀장").build());
        given(positionMapper.findAll(0, 10)).willReturn(positions);
        given(positionMapper.countAll()).willReturn(1);

        PageResult<Position> result = positionService.findAll(0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void update_appliesChange() {
        Position existing = Position.builder().id(1L).name("변경전").allowance(new BigDecimal("0")).active(true).build();
        given(positionMapper.findById(1L)).willReturn(Optional.of(existing));
        given(positionMapper.findByName("변경후")).willReturn(Optional.empty());

        Position result = positionService.update(1L, "변경후", new BigDecimal("150000"));

        assertThat(result.getName()).isEqualTo("변경후");
        assertThat(result.getAllowance()).isEqualByComparingTo("150000");
        verify(positionMapper).update(existing);
    }

    @Test
    void update_toDuplicateName_throws() {
        Position existing = Position.builder().id(1L).name("변경전").active(true).build();
        given(positionMapper.findById(1L)).willReturn(Optional.of(existing));
        given(positionMapper.findByName("이미있음"))
                .willReturn(Optional.of(Position.builder().id(2L).name("이미있음").build()));

        assertThatThrownBy(() -> positionService.update(1L, "이미있음", BigDecimal.ZERO))
                .isInstanceOf(PositionAlreadyExistsException.class);
    }

    /** 자기 자신과 같은 이름으로 "변경"하는 건 중복이 아니다 - id가 같으면 통과해야 한다. */
    @Test
    void update_toSameNameAsSelf_doesNotThrow() {
        Position existing = Position.builder().id(1L).name("팀장").active(true).build();
        given(positionMapper.findById(1L)).willReturn(Optional.of(existing));
        given(positionMapper.findByName("팀장")).willReturn(Optional.of(existing));

        Position result = positionService.update(1L, "팀장", new BigDecimal("200000"));

        assertThat(result.getName()).isEqualTo("팀장");
    }

    @Test
    void deactivate_setsActiveFalse() {
        Position position = Position.builder().id(1L).name("팀장").active(true).build();
        given(positionMapper.findById(1L)).willReturn(Optional.of(position));

        positionService.deactivate(1L);

        assertThat(position.isActive()).isFalse();
        verify(positionMapper).update(position);
    }
}
