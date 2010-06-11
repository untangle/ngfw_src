/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.mail.web.euv.tags;

import javax.servlet.ServletRequest;


/**
 * Includes/excludes body chunks if there
 * is (or could be) a Safelist for the current user
 */
@SuppressWarnings("serial")
public final class HasSafelistTag extends IfElseTag {

    private static final String HAS_ENTRY_KEY = "untangle.safelist.exists";

    @Override
    protected boolean isConditionTrue() {
        Boolean ret = (Boolean) pageContext.getRequest().getAttribute(HAS_ENTRY_KEY);
        return ret==null?
            false:
        ret.booleanValue();
    }

    public static void setCurrent(ServletRequest req, boolean hasEntry) {
        req.setAttribute(HAS_ENTRY_KEY,
                         hasEntry?Boolean.TRUE:Boolean.FALSE);
    }
}
