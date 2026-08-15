package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.ItemSpecRequest;
import com.jinbo.myerp.controller.dto.ItemSpecResponse;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.service.ItemSpecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/item-specs")
@RequiredArgsConstructor
public class ItemSpecController {

    private final ItemSpecService itemSpecService;

    @PutMapping("/{id}")
    public ResponseEntity<ItemSpecResponse> update(@PathVariable Long id, @Valid @RequestBody ItemSpecRequest request) {
        ItemSpec updated = itemSpecService.update(id, request.toDomain());
        return ResponseEntity.ok(ItemSpecResponse.from(updated));
    }
}
