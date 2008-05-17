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

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.SkinSettings;

public class SkinTag extends SimpleTagSupport
{
    static enum SkinType {
        ADMIN( "admin" ),
        USER( "user" );
        
        private final String name;
        private SkinType( String name )
        {
            this.name = name;
        }

        public String getName()
        {
            return this.name;
        }
    };

    private static final Map<String, SkinType> NAME_TO_SKIN_TYPE;

    private static final SkinType DEFAULT_SKIN_TYPE = SkinType.ADMIN;

    private SkinType type = null;
    private String name = null;

    /* !!!unused!!! */
    public String getType()
    {
        if ( this.type == null ) return DEFAULT_SKIN_TYPE.getName();
        return this.type.getName();
    }

    public void setType( String newValue )
    {
        this.type = NAME_TO_SKIN_TYPE.get( newValue );
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
            /* Ideally this would take care of everything and load the skin from the UVM context.
             * but since global tags are loaded in a different classloader, they cannot see
             * the UVM classes, and so the template has to pass in the name of the skin. */
            // LocalUvmContext uvm = LocalUvmContextFactory.context();
            // SkinSettings ss = uvm.skinManager().getSkinSettings();

            String skin = this.name;
            
            
//             SkinType type = this.type;
//             if ( type == null ) type = DEFAULT_SKIN_TYPE;
//             switch ( type ) {
//             case ADMIN:
//                 skin = ss.getAdministrationClientSkin();
//                 break;
//             case USER:
//                 skin = ss.getUserPagesSkin();
//                 break;
//             }

            out.println( "\n<style type=\"text/css\">" );
            out.println( "  @import \"/webui/skins/" + skin + "/css/ext-skin.css\"></script>" );
            out.println( "  @import \"/webui/skins/" + skin + "/css/skin.css\"></script>" );
            out.println( "</style>" );

        } catch ( IOException e ) {
            throw new JspException( "Unable to load the skins.", e );
        }
    }

    public void release()
    {
        this.type = null;
        this.name = null;
    }

    static {
        Map <String,SkinType> nameMap = new HashMap<String,SkinType>();

        for ( SkinType t : SkinType.values()) nameMap.put( t.getName(), t );

        NAME_TO_SKIN_TYPE = Collections.unmodifiableMap( nameMap );
    }
}
