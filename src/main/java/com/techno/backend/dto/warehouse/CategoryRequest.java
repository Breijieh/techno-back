package com.techno.backend.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating an item category.
 * Used in POST and PUT endpoints for category management.
 */
public class CategoryRequest {

    @NotBlank(message = "اسم الفئة مطلوب")
    @Size(max = 200, message = "اسم الفئة لا يجب أن يتجاوز 200 حرف")
    private String categoryName;

    @Size(max = 500, message = "وصف الفئة لا يجب أن يتجاوز 500 حرف")
    private String categoryDescription;

    private Boolean isActive;

    public CategoryRequest() {
    }

    public CategoryRequest(String categoryName, String categoryDescription, Boolean isActive) {
        this.categoryName = categoryName;
        this.categoryDescription = categoryDescription;
        this.isActive = isActive;
    }

    public static CategoryRequestBuilder builder() {
        return new CategoryRequestBuilder();
    }

    public static class CategoryRequestBuilder {
        private String categoryName;
        private String categoryDescription;
        private Boolean isActive;

        public CategoryRequestBuilder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public CategoryRequestBuilder categoryDescription(String categoryDescription) {
            this.categoryDescription = categoryDescription;
            return this;
        }

        public CategoryRequestBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public CategoryRequestBuilder() {
        }

        public CategoryRequest build() {
            return new CategoryRequest(categoryName, categoryDescription, isActive);
        }
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
