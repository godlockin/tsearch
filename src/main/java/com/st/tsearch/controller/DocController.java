package com.st.tsearch.controller;

import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.localData.DocLoadConfig;
import com.st.tsearch.model.save.DocSaveResponse;
import com.st.tsearch.model.save.MDocSaveRequest;
import com.st.tsearch.service.IDocLoadingService;
import com.st.tsearch.service.IDocSavingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Restful api for saving docs
 */
@Slf4j
@RestController
public class DocController {

    @Autowired
    private IDocLoadingService documentLoadingService;

    @Autowired
    private IDocSavingService docSavingService;

    /**
     * Command the system to load docs in target folder and index the tokens
     * @param config file load config
     * @return whether the data loading job success
     */
    @RequestMapping(value = { "_loadDoc" }, method = RequestMethod.POST)
    public ResponseEntity<Boolean> loadDoc(@RequestBody DocLoadConfig config) {
        return ResponseEntity.ok(documentLoadingService.loadLocalDoc(config));
    }

    @RequestMapping(value = { "_doc" }, method = RequestMethod.POST)
    public ResponseEntity<DocSaveResponse> saveDoc(@RequestBody DocUnit docUnit) {
        return ResponseEntity.ok(docSavingService.singleDocSave(docUnit));
    }

    @RequestMapping(value = { "_mDocs" }, method = RequestMethod.POST)
    public ResponseEntity<DocSaveResponse> saveMultiDocs(@RequestBody MDocSaveRequest request) {
        return ResponseEntity.ok(docSavingService.multiDocsSave(request));
    }
}
