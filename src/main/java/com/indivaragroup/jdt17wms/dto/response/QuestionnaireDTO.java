package com.indivaragroup.jdt17wms.dto.response;

import com.indivaragroup.jdt17wms.dto.utils.OptionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireDTO {
    private String question;
    private List<OptionDTO> options;

}
