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
 * ScriptTag
 */
public class ScriptTag extends SimpleTagSupport
{
    private String src;
    
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
     * doTag
     * @throws JspException
     */
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

    /**
     * release
     */
    public void release()
    {
        this.src = null;
    }
}
