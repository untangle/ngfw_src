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

import java.util.Arrays;
import java.util.Iterator;
import javax.servlet.ServletRequest;


/**
 * Tag which is used to iterate over the list
 * of addresses which are remapped-to the
 * current address
 */
public final class ReceivingRemapsListTag
    extends IteratingTag<String> {

    private static final String KEY = "untangle.remapps.ReceivingRemapsListTag";

    @Override
    protected Iterator<String> createIterator() {
        String[] list = getCurrentList(pageContext.getRequest());

        if(list == null || list.length == 0) {
            return null;
        }
        return Arrays.asList(list).iterator();
    }
    @Override
    protected void setCurrent(String s) {
        ReceivingRemapsEntryTag.setCurrent(pageContext, s);
    }

    public static final void setCurrentList(ServletRequest request,
                                            String[] list) {
        request.setAttribute(KEY, list);
    }
    public static final void clearCurrentList(ServletRequest request) {
        request.removeAttribute(KEY);
    }

    /**
     * Returns null if there are no such messages
     */
    static String[] getCurrentList(ServletRequest request) {
        return (String[]) request.getAttribute(KEY);
    }

    static boolean hasCurrentList(ServletRequest request) {
        String[] list = getCurrentList(request);
        return list != null && list.length > 0;
    }
}
