package com.restaurent.nammakadai.service;

import com.restaurent.nammakadai.dto.MenuItemRequest;
import com.restaurent.nammakadai.dto.MenuItemResponse;
import com.restaurent.nammakadai.entity.MenuItem;
import com.restaurent.nammakadai.exception.ResourceNotFoundException;
import com.restaurent.nammakadai.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final CloudinaryService cloudinaryService;

    public List<MenuItemResponse> getAvailableItems() {
        return menuItemRepository.findByAvailableTrue().stream().map(this::toResponse).toList();
    }

    public List<MenuItemResponse> getAllItems() {
        return menuItemRepository.findAll().stream().map(this::toResponse).toList();
    }

    public MenuItemResponse getItem(Long id) {
        return toResponse(findById(id));
    }

    public MenuItemResponse createItem(MenuItemRequest request, MultipartFile image) {
        MenuItem item = new MenuItem();
        mapToEntity(request, item);
        if (image != null && !image.isEmpty()) {
            item.setImageUrl(cloudinaryService.uploadImage(image));
        }
        return toResponse(menuItemRepository.save(item));
    }

    public MenuItemResponse updateItem(Long id, MenuItemRequest request, MultipartFile image) {
        MenuItem item = findById(id);
        String oldImageUrl = item.getImageUrl();
        mapToEntity(request, item);
        if (image != null && !image.isEmpty()) {
            item.setImageUrl(cloudinaryService.uploadImage(image));
            if (oldImageUrl != null) {
                try { cloudinaryService.deleteImage(oldImageUrl); } catch (Exception ignored) {}
            }
        }
        return toResponse(menuItemRepository.save(item));
    }

    public void deleteItem(Long id) {
        MenuItem item = findById(id);
        if (item.getImageUrl() != null) {
            cloudinaryService.deleteImage(item.getImageUrl());
        }
        menuItemRepository.delete(item);
    }

    public List<MenuItemResponse> getItemsByCategory(String category) {
        return menuItemRepository.findByCategory(category).stream().map(this::toResponse).toList();
    }

    private MenuItem findById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + id));
    }

    private MenuItemResponse toResponse(MenuItem item) {
        MenuItemResponse dto = new MenuItemResponse();
        dto.setItemId(item.getItemId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setCategory(item.getCategory());
        dto.setAvailable(item.getAvailable());
        dto.setImageUrl(item.getImageUrl());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }

    private void mapToEntity(MenuItemRequest request, MenuItem item) {
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
    }
}
