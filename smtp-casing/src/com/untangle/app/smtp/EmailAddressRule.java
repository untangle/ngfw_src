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

    public EmailAddressRule()
    {
        this( null );
    }

    public EmailAddressRule(String addr)
    {
        this.addr = addr;
    }

    public String getAddress()
    {
        return this.addr;
    }

    public void setAddress(String addr)
    {
        this.addr = addr;
    }
}
