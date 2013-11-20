/**
 * $Id$
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
