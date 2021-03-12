package com.st.tsearch.model.localData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocLoadConfig {
    private String folderPath = "";
    private String filePrefix = "";
    private String fileSuffix = "";

    @Override
    public String toString() {
        return String.format("Folder Path:[%s], target file prefix:[%s], suffix:[%s]"
                , folderPath
                , filePrefix
                , fileSuffix
        );
    }
}
