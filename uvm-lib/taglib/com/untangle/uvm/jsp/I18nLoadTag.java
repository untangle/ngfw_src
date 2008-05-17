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
package com.untangle.uvm.jsp;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

public class I18nLoadTag extends SimpleTagSupport
{
    public static final String DEFAULT_BUNDLE = "Messages";

    public String path;
    public String bundle;
    
    public String getPath()
    {
        return this.path;
    }

    public void setPath(String v)
    {
        this.path = v;
    }

    public String getBundle()
    {
        return this.bundle;
    }

    public void setBundle(String v)
    {
        this.bundle = v;
    }
    
    public final void doTag() throws JspException
    {
        String path = this.path;
        String bundle = this.bundle;
        if ( bundle == null ) bundle = DEFAULT_BUNDLE;

        PageContext pageContext = (PageContext)getJspContext();
        ServletRequest request = pageContext.getRequest();

        I18n i18n = I18nFactory.getI18n( this.path, bundle, Thread.currentThread().getContextClassLoader(), 
                                         request.getLocale(), I18nFactory.FALLBACK );
        if ( i18n == null ) throw new JspException( "null i18n for " + this.path + " , " + bundle );

        request.setAttribute( "i18n", i18n );

        release();
    }

    public void release()
    {
        this.path = null;
        this.bundle = null;
    }
}
