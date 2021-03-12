package com.st.tsearch.model.doc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocUnit {
    protected String docId;
    protected String hash;
    protected int docLength;
    protected String content;
    protected long version = -1;

    @Override
    public String toString() {
        return String.format("Doc [%s](%s)", docId, hash);
    }
}
