package com.st.tsearch.controller;

import com.st.tsearch.common.Dictionary;
import com.st.tsearch.common.NodeState;
import com.st.tsearch.model.cluster.NodeInfo;
import com.st.tsearch.service.distribute.NodeInfoSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal tracing api, use for health check, get/set the config and system state.
 */
@Slf4j
@RestController
public class InternalController {

    @Autowired
    private NodeInfoSyncService nodeInfoSyncService;

    /**
     * Common health check for the system
     * @return success status if system is ready
     */
    @RequestMapping(value = {"/", "/health"}, method = RequestMethod.GET)
    public ResponseEntity<NodeInfo> healthCheck() {
        NodeInfo nodeInfo = NodeState.getAsObject("NODE", "NODE_INFO", new NodeInfo(), NodeInfo.class);
        return ResponseEntity.ok(nodeInfo);
    }

    /**
     * Remote ping api, used for ensuring each node is alive
     * @return success status if system is ready
     */
    @RequestMapping(value = { "/" }, method = RequestMethod.POST)
    public ResponseEntity<NodeInfo> remotePing(NodeInfo remoteNodeInfo) {
        nodeInfoSyncService.remotePing(remoteNodeInfo);
        NodeInfo nodeInfo = NodeState.getAsObject("NODE", "NODE_INFO", new NodeInfo(), NodeInfo.class);
        return ResponseEntity.ok(nodeInfo);
    }

    /**
     * Get nodes' status in this cluster
     * @return all the nodes' status maintains in this node
     */
    @RequestMapping(value = { "/_nodes" }, method = RequestMethod.GET)
    public ResponseEntity<ConcurrentHashMap<String, Object>> getNodes() {
        ConcurrentHashMap<String, Object> nodeMap = NodeState.getAsConcurrentHashMap("CLUSTER"
                , "NODE_MAP", new ConcurrentHashMap<>());
        return ResponseEntity.ok(nodeMap);
    }

    /**
     * Get all tokens in system dictionary
     * @return all tokens
     */
    @RequestMapping(value = { "/_dict" }, method = RequestMethod.GET)
    public ResponseEntity<List<String>> loadDict() {
        return ResponseEntity.ok(Dictionary.loadAll());
    }
}
