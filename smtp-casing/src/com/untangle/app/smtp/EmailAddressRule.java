/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.io.Serializable;

/**
 * Class used to hold an email address
 */
@SuppressWarnings("serial")
public class EmailAddressRule implements Serializable
{
    private String addr;

    /**
     * Initalize empty EmailAddressRule instance.
     * @return EmailAddressRule instance
     */
    public EmailAddressRule()
    {
        this( null );
    }

    /**
     * Initalize EmailAddressRule instance with email address.
     * @param addr String containing email address.
     * @return EmailAddressRule instance
     */
    public EmailAddressRule(String addr)
    {
        this.addr = addr;
    }

    /**
     * Return the email address.
     * @return String of email address.
     */
    public String getAddress()
    {
        return this.addr;
    }

    /**
     * Specify the email address.
     * @param addr String of email address.
     */
    public void setAddress(String addr)
    {
        this.addr = addr;
    }
}
