package com.st.tsearch.model.save;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocSaveResponse {
    private String nodeId;
    private List<String> success = new ArrayList<>();
    private List<String> failure = new ArrayList<>();
}
