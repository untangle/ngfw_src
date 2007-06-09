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

import javax.servlet.jsp.PageContext;

import sun.misc.BASE64Encoder;

/**
 *
 * Works with ReceivingRemapsListTag
 *
 */
public final class ReceivingRemapsEntryTag
    extends SingleValueTag {

    private static final String KEY = "untangle.remapping.ReceivingRemapsEntryTag";

    private boolean m_encoded = false;

    public boolean isEncoded() {
        return m_encoded;
    }
    public void setEncoded(boolean encoded) {
        m_encoded = encoded;
    }

    @Override
    protected String getValue() {
        String ret = (String) pageContext.getAttribute(KEY, PageContext.PAGE_SCOPE);
        if(isEncoded()) {
            ret = base64Encode(ret);
        }
        return ret;
    }

    public static void setCurrent(PageContext pageContext, String entry) {
        pageContext.setAttribute(KEY, entry, PageContext.PAGE_SCOPE);
    }

    private String base64Encode(String s) {
        if(s == null) {
            return null;
        }
        try {
            return new BASE64Encoder().encode(s.getBytes());
        }
        catch(Exception ex) {
            return null;
        }
    }
}
