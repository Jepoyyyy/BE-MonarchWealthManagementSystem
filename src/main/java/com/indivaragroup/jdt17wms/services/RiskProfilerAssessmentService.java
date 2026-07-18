package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.constants.RiskConstants;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.dto.request.Answer;
import com.indivaragroup.jdt17wms.dto.request.RiskProfilerDTO;
import com.indivaragroup.jdt17wms.dto.utils.OptionDTO;
import com.indivaragroup.jdt17wms.dto.response.QuestionnaireDTO;
import com.indivaragroup.jdt17wms.dto.response.RiskProfilerResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.QuestionnaireDataDTO;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RiskProfilerAssessmentService {

  private final QuestionnaireDataDTO questionnaireDataDTO;
    private final UserRepository userRepository;

    public RiskProfilerAssessmentService(
                                         QuestionnaireDataDTO questionnaireDataDTO,
                                         UserRepository userRepository) {
      this.questionnaireDataDTO = questionnaireDataDTO;
        this.userRepository = userRepository;
    }

    public List<QuestionnaireDTO> getQuestionnaire() {
        if (questionnaireDataDTO.getData() == null) {
            return List.of();
        }
        return questionnaireDataDTO.getData().stream()
                .map(item -> {
                    List<OptionDTO> responseOptions = List.of();
                    if (item.getOptions() != null) {
                        responseOptions = item.getOptions().stream()
                                .map(opt -> OptionDTO.builder()
                                        .label(opt.getLabel())
                                        .score(opt.getScore())
                                        .build())
                          .toList();
                    }
                    return QuestionnaireDTO.builder()
                            .question(item.getQuestion())
                            .options(responseOptions)
                            .build();
                })
                .toList();
    }

    public RiskProfilerResponseDTO updateProfilerAssessment(RiskProfilerDTO riskProfilerDTO) {

        int expectedSize = questionnaireDataDTO.getData() != null ? questionnaireDataDTO.getData().size() : RiskConstants.DEFAULT_QUESTIONNAIRE_SIZE;
        if (riskProfilerDTO.getAnswers().size() != expectedSize) {
            throw new CoreThrowHandler(ApiError.BAD_REQUEST,"Invalid answers count");
        }

        int rawScore = riskProfilerDTO.getAnswers().stream()
                .mapToInt(Answer::getScore)
                .sum();

        String riskProfile;
        if (rawScore <= RiskConstants.RISK_AVERSE_THRESHOLD) {
            riskProfile = RiskConstants.RISK_AVERSE;
        } else if (rawScore <= RiskConstants.RISK_MODERATE_THRESHOLD) {
            riskProfile = RiskConstants.MODERATE;
        } else {
            riskProfile = RiskConstants.RISK_TAKER;
        }

        int outputScore = rawScore * RiskConstants.RISK_SCALING_FACTOR;

        User user = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new CoreThrowHandler(ApiError.USER_NOT_FOUND));

        user.setRiskProfile(riskProfile);
        user.setQuestionnaireCompleted(true);
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        return RiskProfilerResponseDTO.builder()
                .id(user.getId())
                .riskProfile(user.getRiskProfile())
                .questionnaireCompleted(user.getQuestionnaireCompleted())
                .updatedAt(user.getUpdatedAt())
                .score(outputScore)
                .build();
    }
}
