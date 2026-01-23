package com.techno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated user list response.
 * Includes pagination metadata and user list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserListResponse {

    private List<UserInfoResponse> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer size;
    private Integer number;
    private Boolean hasNext;
    private Boolean hasPrevious;
}

