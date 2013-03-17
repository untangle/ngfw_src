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
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.ServletRequest;


/**
 * If the current address is remapped, this is the address to-which
 * the address is remapped
 */
@SuppressWarnings("serial")
public final class RemappedToTag extends SingleValueTag {

    private static final String KEY = "untangle.remapping.RemappedToTag";

    private boolean m_encoded = false;

    public boolean isEncoded() {
        return m_encoded;
    }
    public void setEncoded(boolean encoded) {
        m_encoded = encoded;
    }

    @Override
    protected String getValue() {
        return getCurrent(pageContext.getRequest());
    }

    public static final void setCurrent(ServletRequest request,
                                        String address) {
        request.setAttribute(KEY, address);
    }
    public static final void clearCurret(ServletRequest request) {
        request.removeAttribute(KEY);
    }

    /**
     * Returns null if there is no remap-to address
     */
    static String getCurrent(ServletRequest request) {
        return (String) request.getAttribute(KEY);
    }

    static boolean hasCurrent(ServletRequest request) {
        return getCurrent(request) != null;
    }
}
