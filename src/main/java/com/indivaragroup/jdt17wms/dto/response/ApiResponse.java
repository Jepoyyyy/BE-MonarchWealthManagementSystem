package com.indivaragroup.jdt17wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ApiResponse<T> {

    @JsonProperty("code")
    private int restApiResponseHttpCode;

    @JsonProperty("result")
    private T restApiResponseResult;

    @JsonProperty("message")
    private String restApiResponseMessage;

    @JsonProperty("error")
    private Map<String, Serializable> restApiResponseError;
}
