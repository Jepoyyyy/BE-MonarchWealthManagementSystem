package com.indivaragroup.jdt17wms.aspects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@SuppressWarnings("java:S3011")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class AuditLogHelper {

    private static final String PACKAGE_PREFIX_JAVA = "java.";
    private static final String PACKAGE_PREFIX_JAVAX = "javax.";

    private static final String PROXY_HIBERNATE = "HibernateProxy";

    private static final String GETTER_PREFIX_GET = "get";
    private static final String GETTER_PREFIX_IS = "is";

    private static final String FIELD_MAPPING_DTO_VISIBILITY = "visibility";
    private static final String FIELD_MAPPING_ENTITY_VISIBLE = "visible";

    private static final String SNAKE_CASE_PATTERN = "([a-z])([A-Z]+)";
    private static final String SNAKE_CASE_REPLACEMENT = "$1_$2";

    static boolean isDto(Object arg) {
        if (arg == null) return false;
        if (arg instanceof UUID) return false;
        String packageName = arg.getClass().getPackageName();
        return !packageName.startsWith(PACKAGE_PREFIX_JAVA) && !packageName.startsWith(PACKAGE_PREFIX_JAVAX);
    }

    static Class<?> getUnproxiedClass(Object entity) {
        if (entity == null) return null;
        Class<?> clazz = entity.getClass();
        if (clazz.getName().contains(PROXY_HIBERNATE)) {
            return clazz.getSuperclass();
        }
        return clazz;
    }

    static Object getPropertyValue(Object obj, String propertyName) {
        if (obj == null) return null;
        String capitalized = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String[] getterNames = {GETTER_PREFIX_GET + capitalized, GETTER_PREFIX_IS + capitalized};
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

    static boolean isChanged(Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) return false;
        if (oldValue == null || newValue == null) return true;
        if (oldValue instanceof BigDecimal bd1 && newValue instanceof BigDecimal bd2) {
            return bd1.compareTo(bd2) != 0;
        }
        if (oldValue instanceof Number number && newValue instanceof Number newNumber) {
            return number.doubleValue() != newNumber.doubleValue();
        }
        if (oldValue.getClass().isEnum() || newValue.getClass().isEnum()) {
            return !oldValue.toString().equalsIgnoreCase(newValue.toString());
        }
        return !oldValue.equals(newValue);
    }

    static String getEntityFieldName(String dtoFieldName) {
        if (FIELD_MAPPING_DTO_VISIBILITY.equals(dtoFieldName)) {
            return FIELD_MAPPING_ENTITY_VISIBLE;
        }
        return dtoFieldName;
    }

    static String getJsonFieldName(Field field) {
        if (field.isAnnotationPresent(JsonProperty.class)) {
            return field.getAnnotation(JsonProperty.class).value();
        }
        return toSnakeCase(field.getName());
    }

    static String toSnakeCase(String camelCase) {
        return camelCase.replaceAll(SNAKE_CASE_PATTERN, SNAKE_CASE_REPLACEMENT).toLowerCase();
    }

    static Field findUnderlyingField(Class<?> clazz, String fieldName) {
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

    static List<Field> getDeclaredFieldsInherited(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}
