package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.exception.CategoryMainNotFoundException;
import com.jinbo.myerp.mapper.CategoryMainMapper;
import com.jinbo.myerp.mapper.CategorySubMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMainMapper categoryMainMapper;
    private final CategorySubMapper categorySubMapper;

    @Transactional
    public CategoryMain registerMain(CategoryMain categoryMain) {
        categoryMain.setActive(true);
        categoryMainMapper.insert(categoryMain);
        return categoryMain;
    }

    public List<CategoryMain> findAllMain() {
        return categoryMainMapper.findAll();
    }

    @Transactional
    public CategorySub registerSub(CategorySub categorySub) {
        categoryMainMapper.findById(categorySub.getCategoryMainId())
                .orElseThrow(() -> new CategoryMainNotFoundException(categorySub.getCategoryMainId()));

        categorySub.setActive(true);
        categorySubMapper.insert(categorySub);
        return categorySub;
    }

    public List<CategorySub> findSubsByMainId(Long categoryMainId) {
        return categorySubMapper.findByCategoryMainId(categoryMainId);
    }

    public List<CategorySub> findAllSub() {
        return categorySubMapper.findAll();
    }
}
