package com.st.tsearch.service;

import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.save.DocSaveResponse;
import com.st.tsearch.model.save.MDocSaveRequest;

/**
 * Interface for document saving services
 */
public interface IDocSavingService {
    DocSaveResponse singleDocSave(DocUnit docUnit);

    DocSaveResponse multiDocsSave(MDocSaveRequest request);
}
