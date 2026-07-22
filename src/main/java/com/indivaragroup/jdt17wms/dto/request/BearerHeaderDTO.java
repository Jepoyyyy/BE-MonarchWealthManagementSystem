package com.indivaragroup.jdt17wms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BearerHeaderDTO {
    private String authHeader;

    public static BearerHeaderDTO valueOf(String authHeader) {
        return new BearerHeaderDTO(authHeader);
    }
}
