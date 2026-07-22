package com.indivaragroup.jdt17wms.aspects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indivaragroup.jdt17wms.constants.FinancesConstants;
import com.indivaragroup.jdt17wms.dto.response.ApiResponse;
import com.indivaragroup.jdt17wms.dto.response.UserDTO;
import com.indivaragroup.jdt17wms.dto.utils.SecurityUtils;
import com.indivaragroup.jdt17wms.models.AuditLog;
import com.indivaragroup.jdt17wms.models.User;
import com.indivaragroup.jdt17wms.models.FinancialProfile;
import com.indivaragroup.jdt17wms.models.Expense;
import com.indivaragroup.jdt17wms.repositories.AssetRepository;
import com.indivaragroup.jdt17wms.repositories.AuditLogRepository;
import com.indivaragroup.jdt17wms.repositories.GoalRepository;
import com.indivaragroup.jdt17wms.repositories.ProductRepository;
import com.indivaragroup.jdt17wms.repositories.UserRepository;
import com.indivaragroup.jdt17wms.repositories.FinancialProfileRepository;
import com.indivaragroup.jdt17wms.repositories.ExpenseRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

@Aspect
@Component
@SuppressWarnings("java:S3011")
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final GoalRepository goalRepository;
    private final AssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final ExpenseRepository expenseRepository;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(AuditLogRepository auditLogRepository,
                          GoalRepository goalRepository,
                          AssetRepository assetRepository,
                          ProductRepository productRepository,
                          UserRepository userRepository,
                          FinancialProfileRepository financialProfileRepository,
                          ExpenseRepository expenseRepository,
                          ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.goalRepository = goalRepository;
        this.assetRepository = assetRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.financialProfileRepository = financialProfileRepository;
        this.expenseRepository = expenseRepository;
        this.objectMapper = objectMapper;
    }

    private record FieldChange(
            String field,
            @JsonProperty("old_value") Object oldValue,
            @JsonProperty("new_value") Object newValue
    ) {}

    @Around("@annotation(auditLogged)")
    public void logAudit(ProceedingJoinPoint joinPoint, AuditLogged auditLogged) throws Throwable {
        String category = auditLogged.category();
        String action = auditLogged.action();

        UUID entityId = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UUID uuid) {
                entityId = uuid;
                break;
            }
        }

        Object oldEntity = null;
        if ("GOAL".equalsIgnoreCase(category) && entityId != null) {
            oldEntity = goalRepository.findById(entityId).orElse(null);
        } else if ("ASSET".equalsIgnoreCase(category) && entityId != null) {
            oldEntity = assetRepository.findById(entityId).orElse(null);
        } else if ("PRODUCT".equalsIgnoreCase(category) && entityId != null) {
            oldEntity = productRepository.findById(entityId).orElse(null);
        } else if ("USER".equalsIgnoreCase(category) && entityId != null) {
            oldEntity = userRepository.findById(entityId).orElse(null);
        } else if ("RISK_PROFILE".equalsIgnoreCase(category)) {
            try {
                UUID currentUserId = SecurityUtils.getCurrentUserId();
                oldEntity = userRepository.findById(currentUserId).orElse(null);
            } catch (Exception e) {
                // Ignore if unauthenticated initially
            }
        }

        Map<String, Object> oldEntitySnapshot = null;
        if ("FINANCES".equalsIgnoreCase(category)) {
            try {
                UUID currentUserId = SecurityUtils.getCurrentUserId();
                FinancialProfile fp = financialProfileRepository.findByUserId(currentUserId).orElse(null);
                if (fp != null) {
                    oldEntitySnapshot = new HashMap<>();
                    oldEntitySnapshot.put(FinancesConstants.MONTHLY_INCOME, fp.getMonthlyIncome());
                    Expense exp = expenseRepository.findByFinancialProfileId(fp.getId()).orElse(null);
                    if (exp != null) {
                        oldEntitySnapshot.put(FinancesConstants.HOUSING_EXPENSES, exp.getHousing());
                        oldEntitySnapshot.put(FinancesConstants.FOOD_EXPENSES, exp.getFood());
                        oldEntitySnapshot.put(FinancesConstants.TRANSPORT_EXPENSES, exp.getTransport());
                        oldEntitySnapshot.put(FinancesConstants.UTILITIES_EXPENSES, exp.getUtilities());
                        oldEntitySnapshot.put(FinancesConstants.HEALTHCARE_EXPENSES, exp.getHealthcare());
                        oldEntitySnapshot.put(FinancesConstants.ENTERTAINMENT_EXPENSES, exp.getEntertainment());
                        oldEntitySnapshot.put(FinancesConstants.INSURANCE_EXPENSES, exp.getInsurance());
                        oldEntitySnapshot.put(FinancesConstants.OTHER_EXPENSES, exp.getOther());
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        } else {
            oldEntitySnapshot = snapshotEntity(oldEntity);
        }

        Object result = joinPoint.proceed();

        UUID userId = null;
        String userName = "SYSTEM";
        try {
            userId = SecurityUtils.getCurrentUserId();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDTO authData) {
                userName = authData.getName();
            }
        } catch (Exception e) {
            // Fallback for unauthenticated/anonymous actions
        }

        String changedValueJson = null;
        if (action.contains("UPDATE") || action.contains("CREATE")) {
            List<FieldChange> changes = new ArrayList<>();
            Object dto = null;
            for (Object arg : joinPoint.getArgs()) {
                if (AuditLogHelper.isDto(arg)) {
                    dto = arg;
                    break;
                }
            }

            if (action.contains("UPDATE")) {
                if ("RISK_PROFILE".equalsIgnoreCase(category)) {
                    if (userId != null) {
                        User updatedUser = userRepository.findById(userId).orElse(null);
                        if (updatedUser != null) {
                            Object oldRiskProfile = oldEntitySnapshot.get("riskProfile");
                            Object oldQuestionnaireCompleted = oldEntitySnapshot.get("questionnaireCompleted");

                            if (AuditLogHelper.isChanged(oldRiskProfile, updatedUser.getRiskProfile())) {
                                changes.add(new FieldChange("risk_profile", oldRiskProfile, updatedUser.getRiskProfile()));
                            }
                            if (AuditLogHelper.isChanged(oldQuestionnaireCompleted, updatedUser.getQuestionnaireCompleted())) {
                                changes.add(new FieldChange("questionnaire_completed", oldQuestionnaireCompleted, updatedUser.getQuestionnaireCompleted()));
                            }
                        }
                    }
                } else if ("FINANCES".equalsIgnoreCase(category) && oldEntitySnapshot != null) {
                    if (userId != null) {
                        FinancialProfile updatedFp = financialProfileRepository.findByUserId(userId).orElse(null);
                        Expense updatedExpense = null;
                        if (updatedFp != null) {
                            updatedExpense = expenseRepository.findByFinancialProfileId(updatedFp.getId()).orElse(null);
                        }

                        Object oldIncome = oldEntitySnapshot.get(FinancesConstants.MONTHLY_INCOME);
                        if (updatedFp != null && AuditLogHelper.isChanged(oldIncome, updatedFp.getMonthlyIncome())) {
                            changes.add(new FieldChange(FinancesConstants.MONTHLY_INCOME, oldIncome, updatedFp.getMonthlyIncome()));
                        }

                        String[] expenseFields = {FinancesConstants.HOUSING_EXPENSES, FinancesConstants.FOOD_EXPENSES, FinancesConstants.TRANSPORT_EXPENSES, FinancesConstants.UTILITIES_EXPENSES, FinancesConstants.HEALTHCARE_EXPENSES, FinancesConstants.ENTERTAINMENT_EXPENSES, FinancesConstants.INSURANCE_EXPENSES, FinancesConstants.OTHER_EXPENSES};
                        if (updatedExpense != null) {
                            for (String fieldName : expenseFields) {
                                Object oldVal = oldEntitySnapshot.get(fieldName);
                                Object newVal = AuditLogHelper.getPropertyValue(updatedExpense, fieldName);
                                if (AuditLogHelper.isChanged(oldVal, newVal)) {
                                    changes.add(new FieldChange(fieldName, oldVal, newVal));
                                }
                            }
                        }
                    }
                } else if (dto != null) {
                    changes = getChanges(dto, oldEntitySnapshot);
                }
            } else if (dto != null) {
                changes = getCreateChanges(dto);
            }

            if (!changes.isEmpty()) {
                changedValueJson = objectMapper.writeValueAsString(changes);
            }
        }

        String details = getDetails(action, result, joinPoint.getArgs(), entityId);

        AuditLog auditLog = AuditLog.builder()
                .userId(userId != null ? userId : SecurityUtils.STATIC_USER_ID)
                .userName(userName)
                .action(action)
                .details(details)
                .category(category)
                .changedValue(changedValueJson)
                .timestamp(Instant.now())
                .build();

        auditLogRepository.save(auditLog);

    }

    private Map<String, Object> snapshotEntity(Object entity) {
        if (entity == null) return Map.of();
        Map<String, Object> snapshot = new HashMap<>();
        Class<?> unproxiedClass = AuditLogHelper.getUnproxiedClass(entity);
        for (Field field : AuditLogHelper.getDeclaredFieldsInherited(unproxiedClass)) {
            try {
                Object value = AuditLogHelper.getPropertyValue(entity, field.getName());
                snapshot.put(field.getName(), value);
            } catch (Exception e) {
                // Ignore
            }
        }
        return snapshot;
    }

    private List<FieldChange> getChanges(Object dto, Map<String, Object> oldEntitySnapshot) {
        List<FieldChange> changes = new ArrayList<>();
        for (Field dtoField : AuditLogHelper.getDeclaredFieldsInherited(dto.getClass())) {
            dtoField.setAccessible(true);
            try {
                Object newValue = dtoField.get(dto);
                if (newValue == null) {
                    continue;
                }

                String entityFieldName = AuditLogHelper.getEntityFieldName(dtoField.getName());
                if (oldEntitySnapshot.containsKey(entityFieldName)) {
                    Object oldValue = oldEntitySnapshot.get(entityFieldName);
                    if (AuditLogHelper.isChanged(oldValue, newValue)) {
                        String jsonFieldName = AuditLogHelper.getJsonFieldName(dtoField);
                        changes.add(new FieldChange(jsonFieldName, oldValue, newValue));
                    }
                }
            } catch (Exception e) {
                // Ignore reflection exceptions
            }
        }
        return changes;
    }

    private List<FieldChange> getCreateChanges(Object dto) {
        List<FieldChange> changes = new ArrayList<>();
        for (Field dtoField : AuditLogHelper.getDeclaredFieldsInherited(dto.getClass())) {
            dtoField.setAccessible(true);
            try {
                Object newValue = dtoField.get(dto);
                if (newValue == null) {
                    continue;
                }
                String jsonFieldName = AuditLogHelper.getJsonFieldName(dtoField);
                changes.add(new FieldChange(jsonFieldName, null, newValue));
            } catch (Exception e) {
                // Ignore reflection exceptions
            }
        }
        return changes;
    }

    private String getDetails(String action, Object result, Object[] args, UUID entityId) {
        String name = "";
        for (Object arg : args) {
            if (arg != null) {
                try {
                    Field nameField = AuditLogHelper.findUnderlyingField(arg.getClass(), "name");
                    if (nameField != null) {
                        nameField.setAccessible(true);
                        Object val = nameField.get(arg);
                        name = val != null ? (String) val : "";
                        break;
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        if (name.isEmpty() && result instanceof ApiResponse<?> apiResponse && apiResponse.getRestApiResponseResult() != null) {
            try {
                Object body = apiResponse.getRestApiResponseResult();
                Field nameField = AuditLogHelper.findUnderlyingField(body.getClass(), "name");
                if (nameField != null) {
                    nameField.setAccessible(true);
                    Object val = nameField.get(body);
                    name = val != null ? (String) val : "";
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        String s = name.isEmpty() ? "" : ": " + name;
        return switch (action) {
            case "CREATE_ASSET" -> "Created Asset" + s;
            case "UPDATE_ASSET" -> "Updated Asset" + s + (entityId != null ? " (ID: " + entityId + ")" : "");
            case "DELETE_ASSET" -> "Deleted Asset" + (entityId != null ? " (ID: " + entityId + ")" : "");
            case "CREATE_GOAL" -> "Created Goal" + s;
            case "UPDATE_GOAL" -> "Updated Goal" + s + (entityId != null ? " (ID: " + entityId + ")" : "");
            case "DELETE_GOAL" -> "Deleted Goal" + (entityId != null ? " (ID: " + entityId + ")" : "");
            case "UPDATE_PRODUCT" -> "Updated Product Visibility" + (entityId != null ? " (ID: " + entityId + ")" : "");
            case "UPDATE_RISK_PROFILE" -> "Updated Risk Profile Questionnaire";
            case "UPDATE_USER_STATUS" -> "Updated User Status" + (entityId != null ? " (ID: " + entityId + ")" : "");
            case "UPDATE_FINANCES" -> "Updated Financial Profile and Expenses";
            default -> action + " action performed";
        };
    }
}

