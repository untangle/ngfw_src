/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.mail.web.euv.tags;


/**
 * Includes/excludes body chunks if the
 * current RowsPerPage option is currently active
 */
public final class RPPIsCurrentOptionSelectedTag
    extends IfElseTag {

    private static final String KEY = "untangle.RPPIsCurrentOptionSelectedTag";

    @Override
    protected boolean isConditionTrue() {
        String currentOption = RPPCurrentOptionTag.getCurrent(pageContext);
        String currentValue = PagnationPropertiesTag.getCurrentRowsPerPAge(pageContext.getRequest());

        return (currentValue==null || currentOption==null)?
            false:
        currentValue.trim().equals(currentOption.trim());
    }
}
