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

import javax.servlet.jsp.tagext.TagSupport;


/**
 * Base class for simple tags which just output
 * a single String value w/o line terminator.
 *
 */
@SuppressWarnings("serial")
public abstract class SingleValueTag
    extends TagSupport {

    /**
     * Access the value as a String.  If there is no value, null
     * may be returned.
     *
     * @return the value
     */
    protected abstract String getValue();

    /**
     * May be overidden.  Defines the behavior if
     * the {@link #getValue value} is null.  Default
     * returns "".
     */
    protected String getValueIfNull() {
        return "";
    }


    public final int doStartTag() {
        String value = getValue();
        if(value == null) {
            value = getValueIfNull();
        }
        if(value == null) {
            value = "null";//I'm not sure if this would piss-off the JspWriter
        }
        try {
            pageContext.getOut().print(value);
        }
        catch (Exception ex) {
            throw new Error("Something went wrong");
        }
        return SKIP_BODY;
    }
}
