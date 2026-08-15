package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.ItemRequest;
import com.jinbo.myerp.controller.dto.ItemResponse;
import com.jinbo.myerp.controller.dto.ItemSpecRequest;
import com.jinbo.myerp.controller.dto.ItemSpecResponse;
import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.service.ItemService;
import com.jinbo.myerp.service.ItemSpecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ItemSpecService itemSpecService;

    @PostMapping
    public ResponseEntity<ItemResponse> register(@Valid @RequestBody ItemRequest request) {
        Item saved = itemService.register(request.toDomain(), request.certificationIdsOrEmpty());
        List<com.jinbo.myerp.domain.Certification> certifications = itemService.findCertifications(saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(saved, certifications));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> findById(@PathVariable Long id) {
        Item item = itemService.findById(id);
        List<com.jinbo.myerp.domain.Certification> certifications = itemService.findCertifications(id);
        return ResponseEntity.ok(ItemResponse.from(item, certifications));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ItemResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(itemService.findAll(page, size),
                item -> ItemResponse.from(item, itemService.findCertifications(item.getId()))));
    }

    @PostMapping("/{itemId}/specs")
    public ResponseEntity<ItemSpecResponse> registerSpec(@PathVariable Long itemId, @Valid @RequestBody ItemSpecRequest request) {
        ItemSpec spec = request.toDomain();
        spec.setItemId(itemId);
        ItemSpec saved = itemSpecService.register(spec);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemSpecResponse.from(saved));
    }

    @GetMapping("/{itemId}/specs")
    public ResponseEntity<List<ItemSpecResponse>> findSpecsByItemId(@PathVariable Long itemId) {
        List<ItemSpecResponse> response = itemSpecService.findByItemId(itemId).stream()
                .map(ItemSpecResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
