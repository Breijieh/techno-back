package com.techno.backend.dto.warehouse;

/**
 * DTO for lightweight category summary.
 * Used in list views and dropdown selections.
 */
public class CategorySummary {

    private Long categoryCode;
    private String categoryName;
    private Boolean isActive;
    private Integer itemCount;

    public CategorySummary() {
    }

    public CategorySummary(Long categoryCode, String categoryName, Boolean isActive, Integer itemCount) {
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.isActive = isActive;
        this.itemCount = itemCount;
    }

    public static CategorySummaryBuilder builder() {
        return new CategorySummaryBuilder();
    }

    public static class CategorySummaryBuilder {
        private Long categoryCode;
        private String categoryName;
        private Boolean isActive;
        private Integer itemCount;

        public CategorySummaryBuilder categoryCode(Long categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public CategorySummaryBuilder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public CategorySummaryBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public CategorySummaryBuilder itemCount(Integer itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public CategorySummary build() {
            return new CategorySummary(categoryCode, categoryName, isActive, itemCount);
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
}
