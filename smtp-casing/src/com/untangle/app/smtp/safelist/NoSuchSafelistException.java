/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

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
