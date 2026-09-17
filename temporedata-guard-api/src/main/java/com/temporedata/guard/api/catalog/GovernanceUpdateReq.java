package com.temporedata.guard.api.catalog;

import lombok.Data;

/**
 * Governance update for a table or a column: sensitivity level / category.
 */
@Data
public class GovernanceUpdateReq {

    private String tableId;

    private String columnId;

    private String dataLevelId;

    private String dataCategoryId;
}