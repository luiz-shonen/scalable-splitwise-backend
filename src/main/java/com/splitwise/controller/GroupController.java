package com.splitwise.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitwise.dto.CreateGroupRequest;
import com.splitwise.dto.GroupResponseDTO;
import com.splitwise.service.GroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Endpoints for managing groups and memberships")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @Operation(summary = "Create a new group", description = "Initializes a group with a set of members.")
    public ResponseEntity<GroupResponseDTO> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.ok(groupService.createGroup(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group details", description = "Returns group info, members, and associated expenses.")
    public ResponseEntity<GroupResponseDTO> getGroup(@PathVariable(name = "id") Long id) {
        return groupService.getGroupById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
