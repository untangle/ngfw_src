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

    /**
     * getSrc
     * @return
     */
    public String getSrc()
    {
        return this.src;
    }

    /**
     * setSrc
     * @param newValue
     */
    public void setSrc( String newValue )
    {
        this.src = newValue;
    }
    
    /**
     * getName
     * @return
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * setName
     * @param newValue
     */
    public void setName( String newValue )
    {
        this.name = newValue;
    }
    
    /**
     * doTag
     * @throws JspException
     */
    public final void doTag() throws JspException
    {
        PageContext pageContext = (PageContext)getJspContext();
        JspWriter out = pageContext.getOut();
                
        try {
            String srcName  = "/skins/" + this.name + "/css/" + this.src;
            out.println( "\n<link type=\"text/css\" rel=\"stylesheet\" href=\"" + srcName + "\" />" );
        } catch ( IOException e ) {
            throw new JspException( "Unable to load the skins.", e );
        }
    }

    /** release */
    public void release()
    {
        this.src = null;
        this.name = null;
    }
}
