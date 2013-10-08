/**
 * $Id: SafelistEntryTag.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.jsp.PageContext;

/**
 * Really dumb tag which just outputs the
 * contents of the current Safelist entry while
 * iterating through a collection of 'em
 * <br><br>
 * Works with SafelistListTag
 *
 */
@SuppressWarnings("serial")
public final class SafelistEntryTag extends SingleValueTag
{
    private static final String ENTRY_KEY = "untangle.safelist.entry";

    private boolean m_encoded = false;

    public boolean isEncoded()
    {
        return m_encoded;
    }

    public void setEncoded(boolean encoded)
    {
        m_encoded = encoded;
    }

    @Override
    protected String getValue()
    {
        String ret = (String) pageContext.getAttribute(ENTRY_KEY, PageContext.PAGE_SCOPE);
        if(isEncoded()) {
            ret = base64Encode(ret);
        }
        return ret;
    }

    public static void setCurrent(PageContext pageContext, String entry)
    {
        pageContext.setAttribute(ENTRY_KEY, entry, PageContext.PAGE_SCOPE);
    }

    private String base64Encode(String s)
    {
        if(s == null) {
            return null;
        }
        try {
            return String.valueOf((new org.apache.commons.codec.binary.Base64()).encode(s.getBytes()));
        }
        catch(Exception ex) {
            return null;
        }
    }
}
