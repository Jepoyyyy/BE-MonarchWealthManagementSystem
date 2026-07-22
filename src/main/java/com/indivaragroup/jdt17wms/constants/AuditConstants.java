package com.indivaragroup.jdt17wms.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditConstants {

    public static final int ADMIN_SUMMARY_AUDIT_LOG_LIMIT = 5;

    public static final String GOAL_CATEGORY = "GOAL";
    public static final String ASSET_CATEGORY = "ASSET";
    public static final String PRODUCT_CATEGORY = "PRODUCT";
    public static final String USER_CATEGORY = "USER";
    public static final String RISK_PROFILE_CATEGORY = "RISK_PROFILE";
    public static final String FINANCES_CATEGORY = "FINANCES";
    public static final String RECOMMENDATION_CATEGORY = "RECOMMENDATION";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class RootAction {
        public static final String CREATE = "CREATE";
        public static final String UPDATE = "UPDATE";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Action {
        public static final String CREATE_ASSET = "CREATE_ASSET";
        public static final String UPDATE_ASSET = "UPDATE_ASSET";
        public static final String DELETE_ASSET = "DELETE_ASSET";
        public static final String CREATE_GOAL = "CREATE_GOAL";
        public static final String UPDATE_GOAL = "UPDATE_GOAL";
        public static final String DELETE_GOAL = "DELETE_GOAL";
        public static final String CREATE_PRODUCT = "CREATE_PRODUCT";
        public static final String UPDATE_PRODUCT = "UPDATE_PRODUCT";
        public static final String UPDATE_RISK_PROFILE = "UPDATE_RISK_PROFILE";
        public static final String UPDATE_USER_STATUS = "UPDATE_USER_STATUS";
        public static final String UPDATE_FINANCES = "UPDATE_FINANCES";
        public static final String AUTO_ALLOCATE_GOALS = "AUTO_ALLOCATE_GOALS";
        public static final String GENERATE_RECOMMENDATIONS = "GENERATE_RECOMMENDATIONS";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Message {
        public static final String CREATED_ASSET = "Created Asset";
        public static final String UPDATED_ASSET = "Updated Asset";
        public static final String DELETED_ASSET = "Deleted Asset";
        public static final String CREATED_GOAL = "Created Goal";
        public static final String UPDATED_GOAL = "Updated Goal";
        public static final String DELETED_GOAL = "Deleted Goal";
        public static final String UPDATED_PRODUCT_VISIBILITY = "Updated Product Visibility";
        public static final String UPDATED_RISK_PROFILE_QUESTIONNAIRE = "Updated Risk Profile Questionnaire";
        public static final String UPDATED_USER_STATUS = "Updated User Status";
        public static final String UPDATED_FINANCIAL_PROFILE_AND_EXPENSES = "Updated Financial Profile and Expenses";
        public static final String ACTION_PERFORMED_SUFFIX = " action performed";
    }
}
