package com.indivaragroup.jdt17wms.aspects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Aspect
@Component
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
    public Object logAudit(ProceedingJoinPoint joinPoint, AuditLogged auditLogged) throws Throwable {
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
                    oldEntitySnapshot.put("monthly_income", fp.getMonthlyIncome());
                    Expense exp = expenseRepository.findByFinancialProfileId(fp.getId()).orElse(null);
                    if (exp != null) {
                        oldEntitySnapshot.put("housing", exp.getHousing());
                        oldEntitySnapshot.put("food", exp.getFood());
                        oldEntitySnapshot.put("transport", exp.getTransport());
                        oldEntitySnapshot.put("utilities", exp.getUtilities());
                        oldEntitySnapshot.put("healthcare", exp.getHealthcare());
                        oldEntitySnapshot.put("entertainment", exp.getEntertainment());
                        oldEntitySnapshot.put("insurance", exp.getInsurance());
                        oldEntitySnapshot.put("other", exp.getOther());
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
                if (isDto(arg)) {
                    dto = arg;
                    break;
                }
            }

            if (action.contains("UPDATE")) {
                if ("RISK_PROFILE".equalsIgnoreCase(category) && oldEntitySnapshot != null) {
                    if (userId != null) {
                        User updatedUser = userRepository.findById(userId).orElse(null);
                        if (updatedUser != null) {
                            Object oldRiskProfile = oldEntitySnapshot.get("riskProfile");
                            Object oldQuestionnaireCompleted = oldEntitySnapshot.get("questionnaireCompleted");

                            if (isChanged(oldRiskProfile, updatedUser.getRiskProfile())) {
                                changes.add(new FieldChange("risk_profile", oldRiskProfile, updatedUser.getRiskProfile()));
                            }
                            if (isChanged(oldQuestionnaireCompleted, updatedUser.getQuestionnaireCompleted())) {
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

                        Object oldIncome = oldEntitySnapshot.get("monthly_income");
                        if (updatedFp != null && isChanged(oldIncome, updatedFp.getMonthlyIncome())) {
                            changes.add(new FieldChange("monthly_income", oldIncome, updatedFp.getMonthlyIncome()));
                        }

                        String[] expenseFields = {"housing", "food", "transport", "utilities", "healthcare", "entertainment", "insurance", "other"};
                        if (updatedExpense != null) {
                            for (String fieldName : expenseFields) {
                                Object oldVal = oldEntitySnapshot.get(fieldName);
                                Object newVal = getPropertyValue(updatedExpense, fieldName);
                                if (isChanged(oldVal, newVal)) {
                                    changes.add(new FieldChange(fieldName, oldVal, newVal));
                                }
                            }
                        }
                    }
                } else if (dto != null && oldEntitySnapshot != null) {
                    changes = getChanges(dto, oldEntitySnapshot);
                }
            } else if (action.contains("CREATE") && dto != null) {
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

        return result;
    }

    private boolean isDto(Object arg) {
        if (arg == null) return false;
        Class<?> clazz = arg.getClass();
        if (clazz.isPrimitive()) return false;
        String packageName = clazz.getPackageName();
        if (packageName.startsWith("java.") || packageName.startsWith("javax.") || packageName.startsWith("jakarta.")) {
            return false;
        }
        if (arg instanceof UUID) return false;

        return true;
    }

    private Class<?> getUnproxiedClass(Object entity) {
        Class<?> clazz = entity.getClass();
        if (clazz.getName().contains("$$HibernateProxy") || clazz.getName().contains("$HibernateProxy")) {
            return clazz.getSuperclass();
        }
        return clazz;
    }

    private Object getPropertyValue(Object obj, String propertyName) {
        if (obj == null) return null;
        String capitalized = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String[] getterNames = {"get" + capitalized, "is" + capitalized};
        for (String getterName : getterNames) {
            try {
                java.lang.reflect.Method method = obj.getClass().getMethod(getterName);
                return method.invoke(obj);
            } catch (Exception e) {
                // Ignore
            }
        }
        try {
            Field field = findUnderlyingField(obj.getClass(), propertyName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private Map<String, Object> snapshotEntity(Object entity) {
        if (entity == null) return Map.of();
        Map<String, Object> snapshot = new HashMap<>();
        Class<?> unproxiedClass = getUnproxiedClass(entity);
        for (Field field : getDeclaredFieldsInherited(unproxiedClass)) {
            try {
                Object value = getPropertyValue(entity, field.getName());
                snapshot.put(field.getName(), value);
            } catch (Exception e) {
                // Ignore
            }
        }
        return snapshot;
    }

    private List<FieldChange> getChanges(Object dto, Map<String, Object> oldEntitySnapshot) {
        List<FieldChange> changes = new ArrayList<>();
        if (dto == null || oldEntitySnapshot == null) {
            return changes;
        }
        for (Field dtoField : getDeclaredFieldsInherited(dto.getClass())) {
            dtoField.setAccessible(true);
            try {
                Object newValue = dtoField.get(dto);
                if (newValue == null) {
                    continue;
                }

                String entityFieldName = getEntityFieldName(dtoField.getName());
                if (oldEntitySnapshot.containsKey(entityFieldName)) {
                    Object oldValue = oldEntitySnapshot.get(entityFieldName);
                    if (isChanged(oldValue, newValue)) {
                        String jsonFieldName = getJsonFieldName(dtoField);
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
        if (dto == null) {
            return changes;
        }
        for (Field dtoField : getDeclaredFieldsInherited(dto.getClass())) {
            dtoField.setAccessible(true);
            try {
                Object newValue = dtoField.get(dto);
                if (newValue == null) {
                    continue;
                }
                String jsonFieldName = getJsonFieldName(dtoField);
                changes.add(new FieldChange(jsonFieldName, null, newValue));
            } catch (Exception e) {
                // Ignore reflection exceptions
            }
        }
        return changes;
    }

    private String getEntityFieldName(String dtoFieldName) {
        if ("visibility".equals(dtoFieldName)) {
            return "visible";
        }
        return dtoFieldName;
    }

    private String getJsonFieldName(Field field) {
        if (field.isAnnotationPresent(JsonProperty.class)) {
            return field.getAnnotation(JsonProperty.class).value();
        }
        return toSnakeCase(field.getName());
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    private boolean isChanged(Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) {
            return false;
        }
        if (oldValue == null || newValue == null) {
            return true;
        }
        if (oldValue instanceof BigDecimal bd1 && newValue instanceof BigDecimal bd2) {
            return bd1.compareTo(bd2) != 0;
        }
        if (oldValue instanceof Number n1 && newValue instanceof Number n2) {
            return n1.doubleValue() != n2.doubleValue();
        }
        if (oldValue.getClass().isEnum() || newValue.getClass().isEnum()) {
            return !oldValue.toString().equalsIgnoreCase(newValue.toString());
        }
        return !oldValue.equals(newValue);
    }

    private String getDetails(String action, Object result, Object[] args, UUID entityId) {
        String name = "";
        for (Object arg : args) {
            if (arg != null) {
                try {
                    Field nameField = findUnderlyingField(arg.getClass(), "name");
                    if (nameField != null) {
                        nameField.setAccessible(true);
                        name = (String) nameField.get(arg);
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
                Field nameField = findUnderlyingField(body.getClass(), "name");
                if (nameField != null) {
                    nameField.setAccessible(true);
                    name = (String) nameField.get(body);
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

    private static Field findUnderlyingField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static List<Field> getDeclaredFieldsInherited(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}
