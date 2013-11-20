/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.ServletRequest;

/**
 * If the current address is remapped, this is the address to-which
 * the address is remapped
 */
@SuppressWarnings("serial")
public final class RemappedToTag extends SingleValueTag
{
    private static final String KEY = "untangle.remapping.RemappedToTag";

    private boolean m_encoded = false;

    public boolean isEncoded() {
        return m_encoded;
    }
    public void setEncoded(boolean encoded) {
        m_encoded = encoded;
    }

    @Override
    protected String getValue() {
        return getCurrent(pageContext.getRequest());
    }

    public static final void setCurrent(ServletRequest request,
                                        String address) {
        request.setAttribute(KEY, address);
    }
    public static final void clearCurret(ServletRequest request) {
        request.removeAttribute(KEY);
    }

    /**
     * Returns null if there is no remap-to address
     */
    static String getCurrent(ServletRequest request) {
        return (String) request.getAttribute(KEY);
    }

    static boolean hasCurrent(ServletRequest request) {
        return getCurrent(request) != null;
    }
}
