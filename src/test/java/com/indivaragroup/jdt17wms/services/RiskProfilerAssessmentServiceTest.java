package com.indivaragroup.jdt17wms.services;

import com.indivaragroup.jdt17wms.constants.RiskConstants;
import com.indivaragroup.jdt17wms.dto.request.RiskProfilerDTO;
import com.indivaragroup.jdt17wms.dto.response.QuestionnaireDTO;
import com.indivaragroup.jdt17wms.dto.response.RiskProfilerResponseDTO;
import com.indivaragroup.jdt17wms.dto.utils.QuestionnaireDataDTO;
import com.indivaragroup.jdt17wms.dto.utils.QuestionnaireItem;
import com.indivaragroup.jdt17wms.exceptions.CoreThrowHandler;
import com.indivaragroup.jdt17wms.dto.utils.ApiError;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.indivaragroup.jdt17wms.dto.response.UserDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskProfilerAssessmentServiceTest {

    @Mock
    private FinancialProfileRepository financialProfileRepository;

    @Mock
    private QuestionnaireDataDTO questionnaireDataDTO;

    @Mock
    private UserRepository userRepository;

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @InjectMocks
    private RiskProfilerAssessmentService riskProfilerAssessmentService;

    @BeforeEach
    void setUp() {
        UserDTO userDTO = UserDTO.builder()
                .id(TEST_USER_ID)
                .email("test@example.com")
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDTO, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void serviceShouldBeInitialized() {
        assertNotNull(riskProfilerAssessmentService);
    }

    @Test
    void getQuestionnaire_shouldReturnMappedList() {
        com.indivaragroup.jdt17wms.dto.utils.OptionDTO option = com.indivaragroup.jdt17wms.dto.utils.OptionDTO.builder()
                .label("Protect my capital")
                .score(0)
                .build();
        QuestionnaireItem item = QuestionnaireItem.builder()
                .id(1)
                .question("Goal?")
                .options(List.of(option))
                .build();
        when(questionnaireDataDTO.getData()).thenReturn(List.of(item));

        List<QuestionnaireDTO> result = riskProfilerAssessmentService.getQuestionnaire();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Goal?", result.getFirst().getQuestion());
        assertEquals(1, result.getFirst().getOptions().size());
        assertEquals("Protect my capital", result.getFirst().getOptions().getFirst().getLabel());
        assertEquals(0, result.getFirst().getOptions().getFirst().getScore());
    }

    @Test
    void getQuestionnaire_whenDataIsNull_returnsEmptyList() {
        when(questionnaireDataDTO.getData()).thenReturn(null);
        List<QuestionnaireDTO> result = riskProfilerAssessmentService.getQuestionnaire();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateProfilerAssessment_shouldCalculateRiskAverse() {
        com.indivaragroup.jdt17wms.dto.request.Answer answer1 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(1).build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer2 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(2).build();
        RiskProfilerDTO request = new RiskProfilerDTO(List.of(answer1, answer2)); // sum = 3

        User user = User.builder().id(TEST_USER_ID).build();
        when(questionnaireDataDTO.getData()).thenReturn(List.of(
                QuestionnaireItem.builder().build(),
                QuestionnaireItem.builder().build()
        ));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiskProfilerResponseDTO response = riskProfilerAssessmentService.updateProfilerAssessment(request);

        assertNotNull(response);
        assertEquals("risk_averse", response.getRiskProfile());
        assertEquals(30, response.getScore());
        assertEquals(true, response.getQuestionnaireCompleted());
    }

    @Test
    void updateProfilerAssessment_shouldCalculateModerate() {
        com.indivaragroup.jdt17wms.dto.request.Answer answer1 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(2).build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer2 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(2).build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer3 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(2).build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer4 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(1).build(); // sum = 7
        RiskProfilerDTO request = new RiskProfilerDTO(List.of(answer1, answer2, answer3, answer4));

        User user = User.builder().id(TEST_USER_ID).build();
        when(questionnaireDataDTO.getData()).thenReturn(List.of(
                QuestionnaireItem.builder().build(),
                QuestionnaireItem.builder().build(),
                QuestionnaireItem.builder().build(),
                QuestionnaireItem.builder().build()
        ));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiskProfilerResponseDTO response = riskProfilerAssessmentService.updateProfilerAssessment(request);

        assertNotNull(response);
        assertEquals("moderate", response.getRiskProfile());
        assertEquals(70, response.getScore());
        assertEquals(true, response.getQuestionnaireCompleted());
    }

    @Test
    void updateProfilerAssessment_shouldCalculateRiskTaker() {
        com.indivaragroup.jdt17wms.dto.request.Answer answer1 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(2).build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer2 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(2).build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer3 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(2).build();
        com.indivaragroup.jdt17wms.dto.request.Answer answer4 = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(2).build(); // sum = 8
        RiskProfilerDTO request = new RiskProfilerDTO(List.of(answer1, answer2, answer3, answer4));

        User user = User.builder().id(TEST_USER_ID).build();
        when(questionnaireDataDTO.getData()).thenReturn(List.of(
                QuestionnaireItem.builder().build(),
                QuestionnaireItem.builder().build(),
                QuestionnaireItem.builder().build(),
                QuestionnaireItem.builder().build()
        ));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiskProfilerResponseDTO response = riskProfilerAssessmentService.updateProfilerAssessment(request);

        assertNotNull(response);
        assertEquals("risk_taker", response.getRiskProfile());
        assertEquals(80, response.getScore());
        assertEquals(true, response.getQuestionnaireCompleted());
    }

    @Test
    void updateProfilerAssessment_shouldThrowBadRequestExceptionOnSizeMismatch() {
        com.indivaragroup.jdt17wms.dto.request.Answer answer = com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(1).build();
        RiskProfilerDTO request = new RiskProfilerDTO(List.of(answer));

        when(questionnaireDataDTO.getData()).thenReturn(List.of(
                QuestionnaireItem.builder().build(),
                QuestionnaireItem.builder().build()
        )); // expected 2 answers, got 1

        assertThrows(CoreThrowHandler.class, () -> {
            riskProfilerAssessmentService.updateProfilerAssessment(request);
        });
    }

    @Test
    void getQuestionnaire_whenQuestionnaireDataDTOIsNull_shouldThrowNullPointerException() {
        RiskProfilerAssessmentService service = new RiskProfilerAssessmentService(null, userRepository);
        assertThrows(NullPointerException.class, service::getQuestionnaire);
    }

    @Test
    void getQuestionnaire_itemOptionsNull_returnsEmptyOptions() {
        QuestionnaireItem item = QuestionnaireItem.builder()
                .id(1)
                .question("Goal?")
                .options(null)
                .build();
        when(questionnaireDataDTO.getData()).thenReturn(List.of(item));

        List<QuestionnaireDTO> result = riskProfilerAssessmentService.getQuestionnaire();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Goal?", result.getFirst().getQuestion());
        assertNotNull(result.getFirst().getOptions());
        assertEquals(0, result.getFirst().getOptions().size());
    }

    @Test
    void updateProfilerAssessment_nullDataUsesDefaultSize() {
        when(questionnaireDataDTO.getData()).thenReturn(null);
        List<com.indivaragroup.jdt17wms.dto.request.Answer> answers = java.util.Collections.nCopies(RiskConstants.DEFAULT_QUESTIONNAIRE_SIZE,
                com.indivaragroup.jdt17wms.dto.request.Answer.builder().score(0).build());
        RiskProfilerDTO request = new RiskProfilerDTO(answers);

        User user = User.builder().id(TEST_USER_ID).build();
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RiskProfilerResponseDTO response = riskProfilerAssessmentService.updateProfilerAssessment(request);
        assertNotNull(response);
        assertEquals("risk_averse", response.getRiskProfile());
        assertEquals(0, response.getScore());
        assertTrue(response.getQuestionnaireCompleted());
    }
}

