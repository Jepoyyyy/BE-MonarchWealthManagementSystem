package com.indivaragroup.jdt17wms.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GoalConstants {

    public static final String CUSTOM_GOAL = "custom";

    public static final Map<String, Integer> GOAL_MAX_MONTHS = Map.of(
            "savings", 18,
            "vacation", 12,
            "vehicle", 48,
            "property", 120,
            "retirement", 420,
            CUSTOM_GOAL, 60
    );
}
