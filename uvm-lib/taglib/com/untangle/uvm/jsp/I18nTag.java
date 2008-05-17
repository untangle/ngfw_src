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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.xnap.commons.i18n.I18n;

public class I18nTag extends BodyTagSupport
{
    public String p[] = new String[4];
    public String value;
    
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
    
    public String getValue()
    {
        return this.value;
    }

    public void setValue(String v)
    {
        this.value = v;
    }

    public final int doStartTag() throws JspException
    {
        return EVAL_BODY_TAG;
    }

    public final int doEndTag() throws JspException
    {
        BodyContent body = getBodyContent();
        String bodyString = body.getString();

        if ( this.value == null ) {
            if ( bodyString == null ) {
                throw new JspException( "Translation string must be in the body or in value." );
            }
            
            setValue( bodyString );
        } else if ( bodyString != null ) {
            throw new JspException( "Translation string cannot be in body and value." );
        }
        
        try {
            /* Actually translate the string */
            I18n i18n = (I18n)pageContext.getRequest().getAttribute( "i18n" );
            for ( int c = 0 ; c < this.p.length ; c++ ) if ( this.p[c] == null ) this.p[c] = "";

            body.getEnclosingWriter().print( i18n.tr( this.value, this.p ));

            return EVAL_PAGE;
            
        } catch ( IOException e ) {
            throw new JspException( "Unable to write translation string.", e );
        } finally {
            for ( int c = 0 ; c < this.p.length ; c++ ) this.p[c] = null;
            this.value = null;
        }
    }

    public void release()
    {
        this.p = null;
        this.value = null;
    }
}
