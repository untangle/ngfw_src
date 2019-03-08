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
 * InlineScriptTag
 */
public class InlineScriptTag extends SimpleTagSupport
{
    /**
     * doTag
     * @throws JspException
     */
    public final void doTag() throws JspException
    {
        PageContext pageContext = (PageContext)getJspContext();
        JspWriter out = pageContext.getOut();
                
        try {
            out.println( "\n<script type=\"text/javascript\">" );
            out.println( "// <![CDATA[" );
            getJspBody().invoke( null );
            out.println( "// ]]>" );
            out.println( "</script>" );
        } catch ( IOException e ) {
            throw new JspException( "Unable to load the skins.", e );
        }
    }
}
