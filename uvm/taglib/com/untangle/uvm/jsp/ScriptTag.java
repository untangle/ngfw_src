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
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class ScriptTag extends SimpleTagSupport
{
    private String src;
    
    public String getSrc()
    {
        return this.src;
    }

    public void setSrc( String newValue )
    {
        this.src = newValue;
    }
    
    public final void doTag() throws JspException
    {
        PageContext pageContext = (PageContext)getJspContext();
        JspWriter out = pageContext.getOut();
                
        try {
            out.println( "<script type=\"text/javascript\" src=\"" + this.src + "\"></script>" );
        } catch ( IOException e ) {
            throw new JspException( "Unable to load the skins.", e );
        }
    }

    public void release()
    {
        this.src = null;
    }
}
