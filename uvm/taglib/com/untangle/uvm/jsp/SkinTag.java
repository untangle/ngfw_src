/**
 * $Id$
 */
package com.untangle.uvm.jsp;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * Ideally this would take care of everything and load the skin from the UVM context.
 * but since global tags are loaded in a different classloader, they cannot see
 * the UVM classes, and so the template has to pass in the name of the skin.
 */

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
