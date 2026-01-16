package com.splitwise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private Long createdById;

    private List<Long> memberIds;
}
