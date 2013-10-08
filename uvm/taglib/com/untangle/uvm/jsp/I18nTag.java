/*
 * $HeadURL: svn://chef/work/src/uvm/taglib/com/untangle/uvm/jsp/I18nTag.java $
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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

@SuppressWarnings("serial")
public class I18nTag extends BodyTagSupport
{
    public String p[] = new String[4];
    
    public String getP0()
    {
        return this.p[0];
    }

    public void setP0(String v)
    {
        this.p[0] = v;
    }

    public String getP1()
    {
        return this.p[1];
    }

    public void setP1(String v)
    {
        this.p[1] = v;
    }

    public String getP2()
    {
        return this.p[2];
    }

    public void setP2(String v)
    {
        this.p[2] = v;
    }

    public String getP3()
    {
        return this.p[3];
    }

    public void setP3(String v)
    {
        this.p[3] = v;
    }
    
    public final int doStartTag() throws JspException
    {
        return EVAL_BODY_BUFFERED;
    }

    @SuppressWarnings("unchecked") //getAttribute
    public final int doEndTag() throws JspException
    {
        BodyContent body = getBodyContent();
        String bodyString = null;

        if ( body != null ) bodyString = body.getString().trim();

        JspWriter out = getPreviousOut();
                
        if ( bodyString == null ) {
            throw new JspException( "Translation string must be in the body." );
        }
        
        try {
            /* Actually translate the string */
            Map<String, String> i18n_map = (Map<String, String>)pageContext.getRequest().getAttribute( "i18n_map" );
            for ( int c = 0 ; c < this.p.length ; c++ ) if ( this.p[c] == null ) this.p[c] = "";

            out.print( tr(bodyString, this.p, i18n_map ));

            return EVAL_PAGE;
            
        } catch ( IOException e ) {
            throw new JspException( "Unable to write translation string.", e );
        } finally {
            for ( int c = 0 ; c < this.p.length ; c++ ) this.p[c] = null;
        }
    }
    
    @SuppressWarnings("unchecked") //getAttribute
    public static String i18n( PageContext pageContext, String value )
    {
        /* Actually translate the string */
        Map<String, String> i18n_map = (Map<String, String>)pageContext.getRequest().getAttribute( "i18n_map" );
        return tr(value, i18n_map);
    }

    public void release()
    {
        this.p = null;
    }
 
    //we should use the methods from I18nUtil but, the I18nTag can not load the utility class at runtime,
    //even if from jsp it can be used fine; do not know why.
    private static String tr(String value, Object[] objects, Map<String, String> i18n_map)
    {
        return MessageFormat.format( tr(value,i18n_map), objects);
    }

    private static String tr(String value, Map<String, String> i18n_map)
    {
        String tr = i18n_map.get(value);
        if (tr == null) {
            tr = value;
        }
        return tr;
    }    
}
