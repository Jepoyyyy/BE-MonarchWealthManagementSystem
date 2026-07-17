package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.aspects.AuditLogged;
import com.indivaragroup.jdt17wms.dto.request.RiskProfilerDTO;
import com.indivaragroup.jdt17wms.dto.response.ApiPath;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.QuestionnaireDTO;
import com.indivaragroup.jdt17wms.dto.response.RiskProfilerResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.ApiSuccess;
import com.indivaragroup.jdt17wms.services.RiskProfilerAssessmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.BASE_PROFILER_PATH)
public class RiskProfilerAssessmentController {

    private final RiskProfilerAssessmentService riskProfilerAssessmentService;

    public RiskProfilerAssessmentController(RiskProfilerAssessmentService riskProfilerAssessmentService) {
        this.riskProfilerAssessmentService = riskProfilerAssessmentService;
    }

    @GetMapping
    public ApiResponse<List<QuestionnaireDTO>> getProfilerAssessment() {
        return ApiResponse.success(ApiSuccess.PROFILER_FETCHED,
                riskProfilerAssessmentService.getQuestionnaire());
    }

    @PutMapping
    @AuditLogged(action = "UPDATE_RISK_PROFILE", category = "RISK_PROFILE")
    public ApiResponse<RiskProfilerResponseDTO> updateProfilerAssessment(
            @Valid @RequestBody RiskProfilerDTO riskProfilerDTO) {
        return ApiResponse.success(ApiSuccess.PROFILER_UPDATED,
                riskProfilerAssessmentService.updateProfilerAssessment(riskProfilerDTO));
    }
}
