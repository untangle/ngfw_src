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

/* Ideally this would take care of everything and load the skin from the UVM context.
 * but since global tags are loaded in a different classloader, they cannot see
 * the UVM classes, and so the template has to pass in the name of the skin. */
// LocalUvmContext uvm = LocalUvmContextFactory.context();
// SkinSettings ss = uvm.skinManager().getSkinSettings();



public class SkinTag extends SimpleTagSupport
{
    private String src = null;
    private String name = null;

    public String getSrc()
    {
        return this.src;
    }

    public void setSrc( String newValue )
    {
        this.src = newValue;
    }
    
    public String getName()
    {
        return this.name;
    }

    public void setName( String newValue )
    {
        this.name = newValue;
    }
    
    public final void doTag() throws JspException
    {
        PageContext pageContext = (PageContext)getJspContext();
        JspWriter out = pageContext.getOut();
                
        try {
            String srcName  = "/skins/" + this.name + "/css/" + this.src;
            out.println( "\n<link type=\"text/css\" rel=\"stylesheet\" href=\"" + srcName + "\"></link>" );
        } catch ( IOException e ) {
            throw new JspException( "Unable to load the skins.", e );
        }
    }

    public void release()
    {
        this.src = null;
        this.name = null;
    }
}
