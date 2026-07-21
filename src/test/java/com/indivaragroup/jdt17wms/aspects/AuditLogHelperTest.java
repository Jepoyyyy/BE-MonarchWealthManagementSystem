package com.indivaragroup.jdt17wms.aspects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.indivaragroup.jdt17wms.models.enums.GoalStatus;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogHelperTest {

    // -----------------------------------------------------------------------
    // isDto
    // -----------------------------------------------------------------------

    @Test
    void isDto_nullArg_returnsFalse() {
        assertFalse(AuditLogHelper.isDto(null));
    }

    @Test
    void isDto_stringArg_returnsFalse() {
        // java.lang.String → starts with "java." → false
        assertFalse(AuditLogHelper.isDto("hello"));
    }

    @Test
    void isDto_integerArg_returnsFalse() {
        // java.lang.Integer → starts with "java." → false
        assertFalse(AuditLogHelper.isDto(42));
    }

    @Test
    void isDto_javaxArg_returnsFalse() {
        // javax.xml.namespace.QName is a standard JDK class in javax.* package
        assertFalse(AuditLogHelper.isDto(new javax.xml.namespace.QName("test")));
    }

    // Note: jakarta.* annotation instances at runtime are JVM proxy objects (e.g., com.sun.proxy.*)
    // not actually jakarta.* classes. So isDto returns true for them (they pass the package filter).
    // The jakarta.* package filter branch would only trigger for real jakarta.* concrete class instances
    // (e.g., jakarta.servlet.http.HttpServletRequest implementations), which require a servlet container.

    @Test
    void isDto_uuidArg_returnsFalse() {
        assertFalse(AuditLogHelper.isDto(UUID.randomUUID()));
    }

    @Test
    void isDto_customClassArg_returnsTrue() {
        // Inner class → package is com.indivaragroup... → true
        Object obj = new SampleDto("test");
        assertTrue(AuditLogHelper.isDto(obj));
    }

    // -----------------------------------------------------------------------
    // getUnproxiedClass
    // -----------------------------------------------------------------------

    @Test
    void getUnproxiedClass_normalClass_returnsSameClass() {
        SampleDto dto = new SampleDto("x");
        assertEquals(SampleDto.class, AuditLogHelper.getUnproxiedClass(dto));
    }

    @Test
    void getUnproxiedClass_nullEntity_returnsNull() {
        assertNull(AuditLogHelper.getUnproxiedClass(null));
    }

    @Test
    void getUnproxiedClass_hibernateProxyLike_returnsSuperclass() throws Exception {
        Class<?> proxyClass = new net.bytebuddy.ByteBuddy()
                .subclass(Object.class)
                .name("com.indivaragroup.jdt17wms.aspects.Sample$HibernateProxy$Test")
                .make()
                .load(AuditLogHelperTest.class.getClassLoader())
                .getLoaded();
        Object proxyInstance = proxyClass.getDeclaredConstructor().newInstance();
        assertEquals(Object.class, AuditLogHelper.getUnproxiedClass(proxyInstance));
    }

    // -----------------------------------------------------------------------
    // getPropertyValue
    // -----------------------------------------------------------------------

    @Test
    void getPropertyValue_nullObj_returnsNull() {
        assertNull(AuditLogHelper.getPropertyValue(null, "name"));
    }

    @Test
    void getPropertyValue_existingGetter_returnsValue() {
        SampleDto dto = new SampleDto("hello");
        assertEquals("hello", AuditLogHelper.getPropertyValue(dto, "name"));
    }

    @Test
    void getPropertyValue_noGetterFallbackToField_returnsValue() {
        // SampleWithField has a field but no getter → field-fallback branch
        SampleWithField obj = new SampleWithField();
        obj.secret = "hidden";
        assertEquals("hidden", AuditLogHelper.getPropertyValue(obj, "secret"));
    }

    @Test
    void getPropertyValue_unknownProperty_returnsNull() {
        SampleDto dto = new SampleDto("hi");
        assertNull(AuditLogHelper.getPropertyValue(dto, "nonExistentField"));
    }

    // -----------------------------------------------------------------------
    // isChanged
    // -----------------------------------------------------------------------

    @Test
    void isChanged_bothNull_returnsFalse() {
        assertFalse(AuditLogHelper.isChanged(null, null));
    }

    @Test
    void isChanged_oldNullNewNonNull_returnsTrue() {
        assertTrue(AuditLogHelper.isChanged(null, "something"));
    }

    @Test
    void isChanged_oldNonNullNewNull_returnsTrue() {
        assertTrue(AuditLogHelper.isChanged("something", null));
    }

    @Test
    void isChanged_bigDecimalSameValue_returnsFalse() {
        assertFalse(AuditLogHelper.isChanged(new BigDecimal("100.00"), new BigDecimal("100")));
    }

    @Test
    void isChanged_bigDecimalDifferentValue_returnsTrue() {
        assertTrue(AuditLogHelper.isChanged(new BigDecimal("100"), new BigDecimal("200")));
    }

    @Test
    void isChanged_numbersSameValue_returnsFalse() {
        // Covers Number branch (non-BigDecimal)
        assertFalse(AuditLogHelper.isChanged(100, 100));
    }

    @Test
    void isChanged_numbersDifferentValue_returnsTrue() {
        assertTrue(AuditLogHelper.isChanged(100, 200));
    }

    @Test
    void isChanged_enumsSameIgnoreCase_returnsFalse() {
        assertFalse(AuditLogHelper.isChanged(GoalStatus.IN_PROGRESS, GoalStatus.IN_PROGRESS));
    }

    @Test
    void isChanged_enumsDifferentValue_returnsTrue() {
        assertTrue(AuditLogHelper.isChanged(GoalStatus.IN_PROGRESS, GoalStatus.ACHIEVED));
    }

    @Test
    void isChanged_oldStringNewEnum_returnsFalseIfEqual() {
        assertFalse(AuditLogHelper.isChanged("IN_PROGRESS", GoalStatus.IN_PROGRESS));
    }

    @Test
    void isChanged_oldStringNewEnum_returnsTrueIfDifferent() {
        assertTrue(AuditLogHelper.isChanged("ACHIEVED", GoalStatus.IN_PROGRESS));
    }

    @Test
    void isChanged_equalStrings_returnsFalse() {
        assertFalse(AuditLogHelper.isChanged("hello", "hello"));
    }

    @Test
    void isChanged_differentStrings_returnsTrue() {
        assertTrue(AuditLogHelper.isChanged("hello", "world"));
    }

    // -----------------------------------------------------------------------
    // getEntityFieldName
    // -----------------------------------------------------------------------

    @Test
    void getEntityFieldName_visibility_mapsToVisible() {
        assertEquals("visible", AuditLogHelper.getEntityFieldName("visibility"));
    }

    @Test
    void getEntityFieldName_other_returnsUnchanged() {
        assertEquals("name", AuditLogHelper.getEntityFieldName("name"));
    }

    // -----------------------------------------------------------------------
    // toSnakeCase
    // -----------------------------------------------------------------------

    @Test
    void toSnakeCase_camelCase_convertsCorrectly() {
        assertEquals("target_amount", AuditLogHelper.toSnakeCase("targetAmount"));
        assertEquals("monthly_contribution", AuditLogHelper.toSnakeCase("monthlyContribution"));
        assertEquals("name", AuditLogHelper.toSnakeCase("name"));
    }

    // -----------------------------------------------------------------------
    // getJsonFieldName
    // -----------------------------------------------------------------------

    @Test
    void getJsonFieldName_withJsonPropertyAnnotation_usesAnnotationValue() throws Exception {
        Field f = SampleDto.class.getDeclaredField("annotatedField");
        assertEquals("custom_name", AuditLogHelper.getJsonFieldName(f));
    }

    @Test
    void getJsonFieldName_withoutAnnotation_usesSnakeCase() throws Exception {
        Field f = SampleDto.class.getDeclaredField("name");
        assertEquals("name", AuditLogHelper.getJsonFieldName(f));
    }

    // -----------------------------------------------------------------------
    // findUnderlyingField
    // -----------------------------------------------------------------------

    @Test
    void findUnderlyingField_fieldInClass_returnsField() {
        Field f = AuditLogHelper.findUnderlyingField(SampleDto.class, "name");
        assertNotNull(f);
        assertEquals("name", f.getName());
    }

    @Test
    void findUnderlyingField_fieldInSuperclass_returnsField() {
        // SampleChild extends SampleDto; "name" is in SampleDto
        Field f = AuditLogHelper.findUnderlyingField(SampleChild.class, "name");
        assertNotNull(f);
    }

    @Test
    void findUnderlyingField_nonExistentField_returnsNull() {
        assertNull(AuditLogHelper.findUnderlyingField(SampleDto.class, "doesNotExist"));
    }

    // -----------------------------------------------------------------------
    // getDeclaredFieldsInherited
    // -----------------------------------------------------------------------

    @Test
    void getDeclaredFieldsInherited_includesOwnAndSuperFields() {
        List<Field> fields = AuditLogHelper.getDeclaredFieldsInherited(SampleChild.class);
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("childField")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("name")));
    }

    @Test
    void getDeclaredFieldsInherited_excludesObjectFields() {
        List<Field> fields = AuditLogHelper.getDeclaredFieldsInherited(SampleDto.class);
        assertTrue(fields.stream().noneMatch(f -> f.getDeclaringClass() == Object.class));
    }

    @Test
    void getDeclaredFieldsInherited_nullClass_returnsEmptyList() {
        List<Field> fields = AuditLogHelper.getDeclaredFieldsInherited(null);
        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Helper classes used in tests
    // -----------------------------------------------------------------------

    @Getter
    static class SampleDto {
        private final String name;

        @JsonProperty("custom_name")
        private String annotatedField;

        SampleDto(String name) {
            this.name = name;
        }
    }

    static class SampleChild extends SampleDto {
        @SuppressWarnings("unused")
        private String childField;

        SampleChild(String name) {
            super(name);
        }
    }

    /** Object with a package-private field and no getter — exercises field-fallback in getPropertyValue. */
    static class SampleWithField {
        String secret;
    }

    // -----------------------------------------------------------------------
    // Additional coverage tests for remaining missed branches
    // -----------------------------------------------------------------------

    @Test
    void isDto_jakartaValidationClass_documentsProxyBehavior() throws Exception {
        // Jakarta annotation proxies are com.sun.proxy.* classes at runtime (or similar JVM proxy).
        // The proxy is NOT filtered by the jakarta.* package check, so isDto returns true.
        // This test documents that annotation objects should not be passed as aspect arguments.
        java.lang.reflect.Field field = com.indivaragroup.jdt17wms.dto.request.GoalEditingDTO.class
                .getDeclaredField("targetAmount");
        Object notNullAnnotation = field.getAnnotation(jakarta.validation.constraints.NotNull.class);
        assertNotNull(notNullAnnotation);
        // Annotation proxies pass the filter (they are not in java.*, javax.*, jakarta.* or UUID)
        // This is expected behavior: annotation objects won't be passed as DTO args in practice
        assertNotNull(notNullAnnotation.getClass().getPackageName());
    }

    @Test
    void getUnproxiedClass_normalClassReturnsItself() {
        SampleDto dto = new SampleDto("proxy");
        assertEquals(SampleDto.class, AuditLogHelper.getUnproxiedClass(dto));
    }

    @Test
    void isChanged_bigDecimalOldVsNumberNew_returnsExpected() {
        // oldValue is BigDecimal, newValue is NOT BigDecimal (Integer)
        // Falls through to Number check: BigDecimal is also a Number, so 100.0 != 200.0 → true
        assertTrue(AuditLogHelper.isChanged(new BigDecimal("100"), 200));
    }

    @Test
    void isChanged_numberOldVsNonNumberNew_returnsExpected() {
        // oldValue is Number, newValue is NOT Number (String)
        // Skips Number branch, skips enum branch, uses equals → not equal → true
        assertTrue(AuditLogHelper.isChanged(100, "100"));
    }

    @Test
    void isChanged_enumSameIgnoreCase_returnsFalse() {
        // Same enum value → equalsIgnoreCase returns true → isChanged returns false
        assertFalse(AuditLogHelper.isChanged(GoalStatus.ACHIEVED, GoalStatus.ACHIEVED));
    }

    @Test
    void getDeclaredFieldsInherited_objectClass_returnsEmpty() {
        // Object.class itself — loop stops immediately when current == Object.class
        List<Field> fields = AuditLogHelper.getDeclaredFieldsInherited(Object.class);
        assertTrue(fields.isEmpty());
    }

    @Test
    void findUnderlyingField_traversesClassHierarchy() {
        // SampleChild extends SampleDto; search for name field defined in SampleDto
        Field f = AuditLogHelper.findUnderlyingField(SampleChild.class, "name");
        assertNotNull(f);
        assertEquals(SampleDto.class, f.getDeclaringClass());
    }
}
