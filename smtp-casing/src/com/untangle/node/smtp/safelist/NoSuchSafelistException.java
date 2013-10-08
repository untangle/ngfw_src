/**
 * $Id: NoSuchSafelistException.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.safelist;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NoSuchSafelistException extends Exception implements Serializable
{

    private final String m_emailAddress;

    public NoSuchSafelistException(String address) {
        super("No safelist for address \"" + address + "\"");
        m_emailAddress = address;
    }

    public String getAddress()
    {
        return m_emailAddress;
    }

}
