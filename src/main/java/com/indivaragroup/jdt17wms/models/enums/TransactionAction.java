package com.indivaragroup.jdt17wms.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum TransactionAction {
    BUY,
    SELL;

  @JsonCreator
  public static TransactionAction fromString(String value) {
    if (value == null) return null;

    return Arrays.stream(values())
      .filter(enumValue -> enumValue.name().equalsIgnoreCase(value))
      .findFirst()
      .orElse(null);
  }
}
