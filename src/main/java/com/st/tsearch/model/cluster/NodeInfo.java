package com.st.tsearch.model.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeInfo {
    private String nodeId = "";
    private String ip = "";
    private Integer port = -1;
    private String url = "";
    private String hostname = "";
    private long upTime = -1L;
    private int pingRetry = 0;
    private long lastPing = -1L;
    private String status = "UNKNOWN";

    public NodeInfo(String url) {
        this.url = url;
    }
}
