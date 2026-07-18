package com.indivaragroup.jdt17wms.controllers;

import com.indivaragroup.jdt17wms.dto.request.RiskProfilerDTO;
import com.indivaragroup.jdt17wms.dto.utils.OptionDTO;
import com.indivaragroup.jdt17wms.dto.response.QuestionnaireDTO;
import com.indivaragroup.jdt17wms.dto.response.RiskProfilerResponseDTO;
import com.indivaragroup.jdt17wms.services.RiskProfilerAssessmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskProfilerAssessmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class RiskProfilerAssessmentControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskProfilerAssessmentService riskProfilerAssessmentService;



    @Test
    void getProfilerAssessment_shouldReturnOk() throws Exception {
        OptionDTO option = OptionDTO.builder()
                .label("Protect my capital")
                .score(0)
                .build();
        QuestionnaireDTO questionnaire = QuestionnaireDTO.builder()
                .question("Goal?")
                .options(List.of(option))
                .build();
        when(riskProfilerAssessmentService.getQuestionnaire()).thenReturn(List.of(questionnaire));

        mockMvc.perform(get("/api/v1/me/profiler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].question").value("Goal?"))
                .andExpect(jsonPath("$.result[0].options[0].label").value("Protect my capital"))
                .andExpect(jsonPath("$.result[0].options[0].score").value(0));
    }

    @Test
    void updateProfilerAssessment_shouldReturnOk() throws Exception {
        com.indivaragroup.jdt17wms.dto.request.Answer answer1 = com.indivaragroup.jdt17wms.dto.request.Answer.builder()
                .questionnaireAnswer("A")
                .score(2)
                .build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer2 = com.indivaragroup.jdt17wms.dto.request.Answer.builder()
                .questionnaireAnswer("B")
                .score(2)
                .build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer3 = com.indivaragroup.jdt17wms.dto.request.Answer.builder()
                .questionnaireAnswer("C")
                .score(2)
                .build(); // sum = 6
        RiskProfilerDTO request = new RiskProfilerDTO(List.of(answer1, answer2, answer3));

        java.util.UUID userId = java.util.UUID.randomUUID();
        RiskProfilerResponseDTO responseDto = RiskProfilerResponseDTO.builder()
                .id(userId)
                .riskProfile("moderate")
                .questionnaireCompleted(true)
                .updatedAt(java.time.Instant.parse("2026-07-08T10:00:00Z"))
                .score(70)
                .build();

        when(riskProfilerAssessmentService.updateProfilerAssessment(org.mockito.ArgumentMatchers.any(RiskProfilerDTO.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/me/profiler")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(userId.toString()))
                .andExpect(jsonPath("$.result.risk_profile").value("moderate"))
                .andExpect(jsonPath("$.result.questionnaire_completed").value(true))
                .andExpect(jsonPath("$.result.score").value(70));
    }
}
