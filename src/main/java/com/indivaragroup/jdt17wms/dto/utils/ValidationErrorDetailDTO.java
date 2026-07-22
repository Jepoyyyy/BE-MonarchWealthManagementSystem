package com.indivaragroup.jdt17wms.dto.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorDetailDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String field;
    private String reason;
    private String type;
}
