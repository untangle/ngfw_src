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
 * Conditionaly includes page chunk if
 * messages are present
 */
public final class HasMessagesTag
    extends IfElseTag {

    private String m_type = null;

    public void setType(String type){
        m_type = type;
    }

    public String getType(){
        return m_type;
    }

    @Override
    public boolean isIncludeIfTrue() {
        return true;
    }

    @Override
    protected boolean isConditionTrue() {
        return MessagesSetTag.hasMessages(pageContext.getRequest(), getType());
    }

}
