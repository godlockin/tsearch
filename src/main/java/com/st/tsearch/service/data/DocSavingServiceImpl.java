package com.st.tsearch.service.data;

import com.st.tsearch.common.NodeState;
import com.st.tsearch.common.utils.DataUtils;
import com.st.tsearch.model.cluster.NodeInfo;
import com.st.tsearch.model.doc.DocUnit;
import com.st.tsearch.model.save.DocSaveResponse;
import com.st.tsearch.model.save.MDocSaveRequest;
import com.st.tsearch.service.IDocSavingService;
import com.st.tsearch.service.distribute.DocSaver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocSavingServiceImpl implements IDocSavingService {

    @Autowired
    private DocSaver docSaver;

    /**
     * Save single data into current node
     *
     * @param docUnit data
     * @return save result
     */
    @Override
    public DocSaveResponse singleDocSave(DocUnit docUnit) {
        return docsSaving(Collections.singletonList(docUnit));
    }

    /**
     * Save multi-data into current node
     *
     * @param request data list
     * @return save result
     */
    @Override
    public DocSaveResponse multiDocsSave(MDocSaveRequest request) {
        List<DocUnit> docUnits = request.getData();
        return docsSaving(docUnits);
    }

    /**
     * The real data save function
     *
     * @param docUnits data list
     * @return save result
     */
    private DocSaveResponse docsSaving(List<DocUnit> docUnits) {
        NodeInfo nodeInfo = NodeState.getAsObject("NODE", "NODE_INFO", new NodeInfo(), NodeInfo.class);
        DocSaveResponse faResponse = DocSaveResponse.builder().nodeId(nodeInfo.getNodeId()).build();
        if (CollectionUtils.isEmpty(docUnits)) {
            log.warn("No docs found");
            return faResponse;
        }

        // generate docId if it doesn't exist
        List<String> docIds = docUnits.stream()
                .peek(unit -> unit.setDocId(StringUtils.isBlank(unit.getDocId()) ? DataUtils.initId() : unit.getDocId()))
                .map(DocUnit::getDocId)
                .collect(Collectors.toList());

        // do docs saving
        ConcurrentHashMap<String, DocSaveResponse> saveData = docSaver.docIndexer(docUnits);
        Optional<MutablePair<String, DocSaveResponse>> optResult = saveData.entrySet().stream()
                .map(e -> MutablePair.of(e.getKey(), e.getValue()))
                .findFirst();

        // if no response from doc saver, we will mark all docs as save failure
        if (!optResult.isPresent()) {
            faResponse.setFailure(docIds);
            return faResponse;
        }

        DocSaveResponse finalResponse = optResult.get().getValue();
        log.info("Saved {} docs by node:[{}], ss:{}, fa:{}"
                , docUnits.size()
                , finalResponse.getNodeId()
                , faResponse.getSuccess().size()
                , faResponse.getFailure().size()
        );
        return finalResponse;
    }
}
