package com.temporedata.guard.api.catalog;

import lombok.Data;

/**
 * Update table / column comment request.
 */
@Data
public class CommentUpdateReq {

    private String tableId;

    private String columnId;

    private String comment;
}