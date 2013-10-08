/**
 * $Id: SingleValueTag.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.jsp.tagext.TagSupport;

/**
 * Base class for simple tags which just output
 * a single String value w/o line terminator.
 *
 */
@SuppressWarnings("serial")
public abstract class SingleValueTag extends TagSupport
{
    /**
     * Access the value as a String.  If there is no value, null
     * may be returned.
     *
     * @return the value
     */
    protected abstract String getValue();

    /**
     * May be overidden.  Defines the behavior if
     * the {@link #getValue value} is null.  Default
     * returns "".
     */
    protected String getValueIfNull() {
        return "";
    }


    public final int doStartTag() {
        String value = getValue();
        if(value == null) {
            value = getValueIfNull();
        }
        if(value == null) {
            value = "null";//I'm not sure if this would piss-off the JspWriter
        }
        try {
            pageContext.getOut().print(value);
        }
        catch (Exception ex) {
            throw new Error("Something went wrong");
        }
        return SKIP_BODY;
    }
}
