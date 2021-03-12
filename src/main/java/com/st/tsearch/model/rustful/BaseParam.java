package com.st.tsearch.model.rustful;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base search param
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseParam {

    /* Query page */
    protected int page = 0;

    /* Page size */
    protected int size = 10;
}
