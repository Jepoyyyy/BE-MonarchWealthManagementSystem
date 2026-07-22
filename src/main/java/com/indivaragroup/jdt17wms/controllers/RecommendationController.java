package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.HealthDTO;
import com.indivaragroup.jdt17wms.dto.response.RecommendationDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.ActionRecommendationService;
import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.constants.AuditConstants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.BASE_USER_ROUTE)
public class RecommendationController {

    private final ActionRecommendationService actionRecommendationService;

  public RecommendationController(ActionRecommendationService actionRecommendationService) {
        this.actionRecommendationService = actionRecommendationService;
  }

    @GetMapping(ApiPath.HEALTH_ROUTE)
    public ApiResponse<HealthDTO> getHealth() {
        return ApiResponse.success(ApiSuccess.HEALTH_OK,
                actionRecommendationService.getHealthScore());
    }

    @PostMapping(ApiPath.RECOMMENDATIONS_ROUTE)
    @AuditLogged(action = AuditConstants.Action.GENERATE_RECOMMENDATIONS, category = AuditConstants.RECOMMENDATION_CATEGORY)
    public ApiResponse<List<RecommendationDTO>> getRecommendations() {
        return ApiResponse.success(ApiSuccess.RECOMMENDATIONS_FETCHED,
                actionRecommendationService.generateRecommendations());
    }
}
