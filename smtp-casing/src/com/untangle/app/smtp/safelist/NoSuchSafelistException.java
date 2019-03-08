/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

import java.io.Serializable;

/**
 * Exception to show safelist address.
 */
@SuppressWarnings("serial")
public class NoSuchSafelistException extends Exception implements Serializable
{

    private final String m_emailAddress;

    /**
     * [NoSuchSafelistException description]
     * @param  address Email address to diplay.
     * @return         Exception containing the email address in the message.
     */
    public NoSuchSafelistException(String address) {
        super("No safelist for address \"" + address + "\"");
        m_emailAddress = address;
    }

    /**
     * Return the email address.
     *
     * @return String of email address.
     */
    public String getAddress()
    {
        return m_emailAddress;
    }

}
