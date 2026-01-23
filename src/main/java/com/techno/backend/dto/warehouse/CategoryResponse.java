package com.techno.backend.dto.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * DTO for item category response data.
 * Returned from category API endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    private Long categoryCode;
    private String categoryName;
    private String categoryDescription;
    private Boolean isActive;
    private Integer itemCount;

    // Audit fields
    private LocalDateTime createdDate;
    private Long createdBy;
    private LocalDateTime modifiedDate;
    private Long modifiedBy;

    public CategoryResponse() {
    }

    public CategoryResponse(Long categoryCode, String categoryName, String categoryDescription,
            Boolean isActive, Integer itemCount, LocalDateTime createdDate,
            Long createdBy, LocalDateTime modifiedDate, Long modifiedBy) {
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.categoryDescription = categoryDescription;
        this.isActive = isActive;
        this.itemCount = itemCount;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.modifiedDate = modifiedDate;
        this.modifiedBy = modifiedBy;
    }

    public static CategoryResponseBuilder builder() {
        return new CategoryResponseBuilder();
    }

    public static class CategoryResponseBuilder {
        private Long categoryCode;
        private String categoryName;
        private String categoryDescription;
        private Boolean isActive;
        private Integer itemCount;
        private LocalDateTime createdDate;
        private Long createdBy;
        private LocalDateTime modifiedDate;
        private Long modifiedBy;

        public CategoryResponseBuilder categoryCode(Long categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public CategoryResponseBuilder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public CategoryResponseBuilder categoryDescription(String categoryDescription) {
            this.categoryDescription = categoryDescription;
            return this;
        }

        public CategoryResponseBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public CategoryResponseBuilder itemCount(Integer itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public CategoryResponseBuilder createdDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public CategoryResponseBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public CategoryResponseBuilder modifiedDate(LocalDateTime modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public CategoryResponseBuilder modifiedBy(Long modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public CategoryResponse build() {
            return new CategoryResponse(categoryCode, categoryName, categoryDescription, isActive, itemCount,
                    createdDate, createdBy, modifiedDate, modifiedBy);
        }
    }

    public Long getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(Long categoryCode) {
        this.categoryCode = categoryCode;
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

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getStatusDisplay() {
        return Boolean.TRUE.equals(isActive) ? "Active" : "Inactive";
    }

    public String getStatusColor() {
        return Boolean.TRUE.equals(isActive) ? "green" : "gray";
    }
}
