package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.mapper.PartnerMapper;
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
class PartnerServiceTest {

    @Mock
    private PartnerMapper partnerMapper;

    @InjectMocks
    private PartnerService partnerService;

    @Test
    void register_setsActiveTrue() {
        Partner partner = Partner.builder().name("동양건재상사").partnerType(PartnerType.CUSTOMER).build();

        Partner result = partnerService.register(partner);

        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();
        verify(partnerMapper).insert(partner);
    }

    @Test
    void findById_notFound_throws() {
        given(partnerMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> partnerService.findById(1L))
                .isInstanceOf(PartnerNotFoundException.class);
    }

    @Test
    void findAll_returnsPageResult() {
        List<Partner> partners = List.of(Partner.builder().id(1L).name("a").build());
        given(partnerMapper.findAll(0, 10)).willReturn(partners);
        given(partnerMapper.countAll()).willReturn(1);

        PageResult<Partner> result = partnerService.findAll(0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
    }

    @Test
    void update_appliesChanges() {
        Partner existing = Partner.builder().id(1L).name("변경전").partnerType(PartnerType.CUSTOMER).active(true).build();
        Partner changes = Partner.builder().name("변경후").partnerType(PartnerType.SUPPLIER).build();
        given(partnerMapper.findById(1L)).willReturn(Optional.of(existing));

        Partner result = partnerService.update(1L, changes);

        assertThat(result.getName()).isEqualTo("변경후");
        assertThat(result.getPartnerType()).isEqualTo(PartnerType.SUPPLIER);
        verify(partnerMapper).update(existing);
    }

    @Test
    void deactivate_setsActiveFalse() {
        Partner partner = Partner.builder().id(1L).name("a").active(true).build();
        given(partnerMapper.findById(1L)).willReturn(Optional.of(partner));

        partnerService.deactivate(1L);

        assertThat(partner.isActive()).isFalse();
        verify(partnerMapper).update(partner);
    }
}
