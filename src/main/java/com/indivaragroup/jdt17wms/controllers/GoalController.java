package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.constants.AuditConstants;
import com.indivaragroup.jdt17wms.dto.request.GoalEditingDTO;
import com.indivaragroup.jdt17wms.dto.request.GoalRegistrationDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.GoalDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalProgressResponseDTO;
import com.indivaragroup.jdt17wms.dto.response.GoalProjectionDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.GoalsManagementService;
import com.indivaragroup.jdt17wms.services.GoalProgressService;
import com.indivaragroup.jdt17wms.services.GoalsProjectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping(ApiPath.BASE_GOALS_ROUTE)
public class GoalController {

    private final GoalsManagementService goalsManagementService;
    private final GoalsProjectionService goalsProjectionService;
    private final GoalProgressService goalProgressService;

    public GoalController(GoalsManagementService goalsManagementService,
                          GoalsProjectionService goalsProjectionService,
                          GoalProgressService goalProgressService) {
        this.goalsManagementService = goalsManagementService;
        this.goalsProjectionService = goalsProjectionService;
        this.goalProgressService = goalProgressService;
    }

    @GetMapping
    public ApiResponse<List<GoalDTO>> getGoals() {
        return ApiResponse.success(ApiSuccess.GOALS_FETCHED,
                goalsManagementService.getGoalsForUser());
    }

    @PostMapping
    @AuditLogged(action = AuditConstants.Action.CREATE_GOAL, category = AuditConstants.GOAL_CATEGORY)
    public ApiResponse<GoalDTO> createGoal(@Valid @RequestBody GoalRegistrationDTO dto) {
        return ApiResponse.created(ApiSuccess.GOAL_CREATED,
                goalsManagementService.createGoalForUser(dto));
    }

    @PutMapping(ApiPath.ID_SLUG)
    @AuditLogged(action = AuditConstants.Action.UPDATE_GOAL, category = AuditConstants.GOAL_CATEGORY)
    public ApiResponse<GoalDTO> updateGoal(@PathVariable UUID id,
                                            @Valid @RequestBody GoalEditingDTO dto) {
        return ApiResponse.success(ApiSuccess.GOAL_UPDATED,
                goalsManagementService.updateGoalForUser(id, dto));
    }

    @DeleteMapping(ApiPath.ID_SLUG)
    @AuditLogged(action = AuditConstants.Action.DELETE_GOAL, category = AuditConstants.GOAL_CATEGORY)
    public ApiResponse<Void> deleteGoal(@PathVariable UUID id) {
        goalsManagementService.deleteGoalForUser(id);
        return ApiResponse.success(ApiSuccess.GOAL_DELETED, null);
    }

    @GetMapping(ApiPath.PROJECTIONS_ROUTE)
    public ApiResponse<List<GoalProjectionDTO>> getGoalProjections() {
        return ApiResponse.success(ApiSuccess.GOAL_PROJECTIONS_FETCHED,
                goalsProjectionService.getProjectionsForUser());
    }

    @PostMapping(ApiPath.AUTO_ALLOCATE_ROUTE)
    @AuditLogged(action = AuditConstants.Action.AUTO_ALLOCATE_GOALS, category = AuditConstants.GOAL_CATEGORY)
    public ApiResponse<List<GoalDTO>> autoAllocate(
            @RequestParam
            @Min(value = 1, message = "Percentage must be between 1 and 100")
            @Max(value = 100, message = "Percentage must be between 1 and 100")
            int percentage) {
        return ApiResponse.success(ApiSuccess.GOALS_FETCHED,
                goalsManagementService.autoAllocateGoalsForUser(percentage));
    }

    @GetMapping(ApiPath.PROGRESS_ROUTE)
    public ApiResponse<List<GoalProgressResponseDTO>> getGoalProgress() {
        return ApiResponse.success(ApiSuccess.GOAL_PROGRESS_FETCHED,
                goalProgressService.getGoalProgressForUser());
    }
}
