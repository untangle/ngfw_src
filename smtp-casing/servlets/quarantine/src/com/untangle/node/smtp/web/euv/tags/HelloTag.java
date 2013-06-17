/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * This is a test tag
 */
@SuppressWarnings("serial")
public class HelloTag extends
                          TagSupport {

    private String m_message = null;

    public void setMessage(String value){
        m_message = value;
    }

    public String getMessage(){
        return m_message;
    }


    public int doStartTag() {
        try {
            JspWriter out = pageContext.getOut();
            out.println(m_message);
        }
        catch (Exception ex) {
            throw new Error("Something went wrong");
        }
        return SKIP_BODY;
    }
}
