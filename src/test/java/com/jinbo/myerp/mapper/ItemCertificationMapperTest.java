package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.domain.Item;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class ItemCertificationMapperTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    @Autowired
    private CategorySubMapper categorySubMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private CertificationMapper certificationMapper;

    @Autowired
    private ItemCertificationMapper itemCertificationMapper;

    private Long insertItem() {
        CategoryMain main = CategoryMain.builder().name("타일/건축용접착제").displayOrder(1).active(true).build();
        categoryMainMapper.insert(main);
        CategorySub sub = CategorySub.builder().categoryMainId(main.getId()).name("내장타일 접착제").displayOrder(1).active(true).build();
        categorySubMapper.insert(sub);
        LocalDateTime now = LocalDateTime.now();
        Item item = Item.builder().categorySubId(sub.getId()).name("세라픽스 PC-7000D").active(true).createdAt(now).updatedAt(now).build();
        itemMapper.insert(item);
        return item.getId();
    }

    @Test
    void insertAndFindCertificationsByItemId() {
        Long itemId = insertItem();
        Certification cert1 = Certification.builder().name("KS인증").build();
        Certification cert2 = Certification.builder().name("친환경인증").build();
        certificationMapper.insert(cert1);
        certificationMapper.insert(cert2);

        itemCertificationMapper.insert(itemId, cert1.getId());
        itemCertificationMapper.insert(itemId, cert2.getId());

        List<Certification> certifications = itemCertificationMapper.findCertificationsByItemId(itemId);

        assertThat(certifications).hasSize(2);
        assertThat(certifications).extracting(Certification::getName)
                .containsExactlyInAnyOrder("KS인증", "친환경인증");
    }

    @Test
    void deleteByItemId_removesAllAssociations() {
        Long itemId = insertItem();
        Certification cert = Certification.builder().name("KS인증").build();
        certificationMapper.insert(cert);
        itemCertificationMapper.insert(itemId, cert.getId());

        itemCertificationMapper.deleteByItemId(itemId);

        List<Certification> certifications = itemCertificationMapper.findCertificationsByItemId(itemId);
        assertThat(certifications).isEmpty();
    }
}
