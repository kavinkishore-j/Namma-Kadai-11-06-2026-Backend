package com.restaurent.nammakadai.controller;

import com.restaurent.nammakadai.dto.MenuItemRequest;
import com.restaurent.nammakadai.dto.MenuItemResponse;
import com.restaurent.nammakadai.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getAvailableMenu() {
        return ResponseEntity.ok(menuService.getAvailableItems());
    }

    @GetMapping("/all")
    public ResponseEntity<List<MenuItemResponse>> getAllMenu() {
        return ResponseEntity.ok(menuService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getItem(id));
    }

    @GetMapping("/getByCategory")
    public ResponseEntity<List<MenuItemResponse>> getItemByCategory(@RequestParam String category) {
        return ResponseEntity.ok(menuService.getItemsByCategory(category));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_MENU')")
    public ResponseEntity<MenuItemResponse> createItem(@RequestPart("data") @Valid MenuItemRequest request,
                                                       @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createItem(request, image));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('UPDATE_MENU')")
    public ResponseEntity<MenuItemResponse> updateItem(@PathVariable Long id,
                                                       @RequestPart("data") @Valid MenuItemRequest request,
                                                       @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(menuService.updateItem(id, request, image));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        menuService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
