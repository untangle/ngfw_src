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
package com.untangle.node.mail.web.euv.tags;


/**
 * Includes/excludes body chunks if there
 * is an index
 */
public final class HasInboxIndexTag
    extends IfElseTag {

    @Override
    protected boolean isConditionTrue() {
        return InboxIndexTag.hasCurrentIndex(pageContext.getRequest());
    }
}
