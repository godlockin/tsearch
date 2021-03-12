package com.st.tsearch.service;

import com.st.tsearch.model.localData.DocLoadConfig;

/**
 * Interface for document loading services
 */
public interface IDocLoadingService {
    boolean loadLocalDoc(DocLoadConfig config);
}
