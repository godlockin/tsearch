package com.st.tsearch.model.localData;

import com.st.tsearch.model.doc.DocUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUnit extends DocUnit {
    private String title;
    private String fileName;
    private String filePath;
}
