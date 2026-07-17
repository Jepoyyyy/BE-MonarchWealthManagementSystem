package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.HealthDTO;
import com.indivaragroup.jdt17wms.dto.response.RecommendationDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.ActionRecommendationService;
import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.BASE_USER_PATH)
public class RecommendationController {

    private final ActionRecommendationService actionRecommendationService;

  public RecommendationController(ActionRecommendationService actionRecommendationService) {
        this.actionRecommendationService = actionRecommendationService;
  }

    @GetMapping("/health")
    public ApiResponse<HealthDTO> getHealth() {
        return ApiResponse.success(ApiSuccess.HEALTH_OK,
                actionRecommendationService.getHealthScore());
    }

    @PostMapping("/recommendations")
    @AuditLogged(action = "GENERATE_RECOMMENDATIONS", category = "RECOMMENDATION")
    public ApiResponse<List<RecommendationDTO>> getRecommendations() {
        return ApiResponse.success(ApiSuccess.RECOMMENDATIONS_FETCHED,
                actionRecommendationService.generateRecommendations());
    }
}
