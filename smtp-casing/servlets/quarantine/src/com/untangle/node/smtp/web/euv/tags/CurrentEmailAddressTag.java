/**
 * $Id: CurrentEmailAddressTag.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.web.euv.tags;

import javax.servlet.ServletRequest;


/**
 * Outputs the current email address, or null
 * if there 'aint one
 */
@SuppressWarnings("serial")
public final class CurrentEmailAddressTag extends SingleValueTag {

    private static final String ADDRESS_KEY = "untangle.email_address";
    private static final String EL_ADDRESS_KEY = "currentAddress";

    @Override
    protected String getValue() {
        return getCurrent(pageContext.getRequest());
    }

    public static final void setCurrent(ServletRequest request,
                                        String address) {
        request.setAttribute(ADDRESS_KEY, address);
        request.setAttribute(EL_ADDRESS_KEY, address);
    }
    public static final void clearCurrent(ServletRequest request) {
        request.removeAttribute(ADDRESS_KEY);
        request.removeAttribute(EL_ADDRESS_KEY);
    }

    /**
     * Returns null if there is no current address
     */
    static String getCurrent(ServletRequest request) {
        return (String) request.getAttribute(ADDRESS_KEY);
    }

    static boolean hasCurrent(ServletRequest request) {
        return getCurrent(request) != null;
    }
}
