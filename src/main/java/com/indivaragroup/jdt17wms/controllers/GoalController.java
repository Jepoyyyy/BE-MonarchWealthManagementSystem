package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.dto.request.GoalEditingDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.GoalDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalProjectionDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.GoalsManagementService;
import com.indivaragroup.jdt17wms.services.GoalsProjectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.BASE_GOALS_PATH)
public class GoalController {

    private final GoalsManagementService goalsManagementService;
    private final GoalsProjectionService goalsProjectionService;

    public GoalController(GoalsManagementService goalsManagementService,
                          GoalsProjectionService goalsProjectionService) {
        this.goalsManagementService = goalsManagementService;
        this.goalsProjectionService = goalsProjectionService;
    }

    @GetMapping
    public ApiResponse<List<GoalDTO>> getGoals() {
        return ApiResponse.success(ApiSuccess.GOALS_FETCHED,
                goalsManagementService.getGoalsForUser());
    }

    @PostMapping
    @AuditLogged(action = "CREATE_GOAL", category = "GOAL")
    public ApiResponse<GoalDTO> createGoal(@Valid @RequestBody GoalRegistrationDTO dto) {
        return ApiResponse.created(ApiSuccess.GOAL_CREATED,
                goalsManagementService.createGoalForUser(dto));
    }

    @PutMapping("/{id}")
    @AuditLogged(action = "UPDATE_GOAL", category = "GOAL")
    public ApiResponse<GoalDTO> updateGoal(@PathVariable UUID id,
                                            @Valid @RequestBody GoalEditingDTO dto) {
        return ApiResponse.success(ApiSuccess.GOAL_UPDATED,
                goalsManagementService.updateGoalForUser(id, dto));
    }

    @DeleteMapping("/{id}")
    @AuditLogged(action = "DELETE_GOAL", category = "GOAL")
    public ApiResponse<Void> deleteGoal(@PathVariable UUID id) {
        goalsManagementService.deleteGoalForUser(id);
        return ApiResponse.success(ApiSuccess.GOAL_DELETED, null);
    }

    @GetMapping("/projections")
    public ApiResponse<List<GoalProjectionDTO>> getGoalProjections() {
        return ApiResponse.success(ApiSuccess.GOAL_PROJECTIONS_FETCHED,
                goalsProjectionService.getProjectionsForUser());
    }
}
