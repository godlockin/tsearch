package com.st.tsearch.model.save;

import com.st.tsearch.model.doc.DocUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MDocSaveRequest {
    private List<DocUnit> data;
}
