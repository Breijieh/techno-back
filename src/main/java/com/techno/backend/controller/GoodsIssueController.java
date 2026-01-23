package com.techno.backend.controller;

import com.techno.backend.dto.ApiResponse;
import com.techno.backend.dto.warehouse.GoodsIssueRequest;
import com.techno.backend.dto.warehouse.GoodsIssueResponse;
import com.techno.backend.service.GoodsIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse/issuing")
@RequiredArgsConstructor
@Slf4j
public class GoodsIssueController {

    private final GoodsIssueService goodsIssueService;

    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN') or hasRole('PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<GoodsIssueResponse>> createGoodsIssue(
            @Valid @RequestBody GoodsIssueRequest request) {
        log.info("REST request to create goods issue for store: {}, project: {}", request.getStoreCode(), request.getProjectCode());

        GoodsIssueResponse response = goodsIssueService.createGoodsIssue(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("تم إنشاء إصدار البضائع بنجاح", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<List<GoodsIssueResponse>>> getAllGoodsIssues(
            @RequestParam(required = false) Long projectCode,
            @RequestParam(required = false) Long storeCode) {
        log.info("REST request to get all goods issues - projectCode: {}, storeCode: {}", projectCode, storeCode);

        List<GoodsIssueResponse> issues = goodsIssueService.getAllGoodsIssues();

        // Filter by projectCode if provided
        if (projectCode != null) {
            issues = issues.stream()
                    .filter(i -> i.getProjectCode() != null && i.getProjectCode().equals(projectCode))
                    .toList();
        }

        // Filter by storeCode if provided
        if (storeCode != null) {
            issues = issues.stream()
                    .filter(i -> i.getStoreCode().equals(storeCode))
                    .toList();
        }

        return ResponseEntity.ok(ApiResponse.success(issues));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER', 'PROJECT_MANAGER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<GoodsIssueResponse>> getGoodsIssueById(@PathVariable Long id) {
        log.info("REST request to get goods issue: {}", id);

        GoodsIssueResponse response = goodsIssueService.getGoodsIssueById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GoodsIssueResponse>> updateGoodsIssue(
            @PathVariable Long id,
            @Valid @RequestBody GoodsIssueRequest request) {
        log.info("REST request to update goods issue: {}", id);

        GoodsIssueResponse response = goodsIssueService.updateGoodsIssue(id, request);

        return ResponseEntity.ok(ApiResponse.success("تم تحديث إصدار البضائع بنجاح", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGoodsIssue(@PathVariable Long id) {
        log.info("REST request to delete goods issue: {}", id);

        goodsIssueService.deleteGoodsIssue(id);

        return ResponseEntity.ok(ApiResponse.success("تم حذف إصدار البضائع بنجاح", null));
    }
}

