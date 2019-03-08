/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.io.Serializable;

/**
 * Class used to associate two email addresses
 */
@SuppressWarnings("serial")
public class EmailAddressPairRule implements Serializable
{
    private String addr1;
    private String addr2;

    /**
     * Initiaize EmailAddressPairRule instance with empty values.
     *
     * @return EmailAddressPairRule instance.
     */
    public EmailAddressPairRule() {
        this(null, null);
    }

    /**
     * Initiaize EmailAddressPairRule instance with addresses.
     *
     * @param addr1 String of email address.
     * @param addr2 String of email address.
     * @return EmailAddressPairRule instance.
     */
    public EmailAddressPairRule(String addr1, String addr2) {
        this.addr1 = addr1;
        this.addr2 = addr2;
    }

    /**
     * Set the first email address.
     * @param addr1 String of email address.
     */
    public void setAddress1(String addr1)
    {
        this.addr1 = addr1;
    }

    /**
     * Retrieve the first email address.
     * @return String of email address.
     */
    public String getAddress1()
    {
        return this.addr1;
    }

    /**
     * Set the second email address.
     * @param addr2 String of email address.
     */
    public void setAddress2(String addr2)
    {
        this.addr2 = addr2;
    }

    /**
     * Retrieve the second email address.
     * @return String of email address.
     */
    public String getAddress2()
    {
        return this.addr2;
    }
}
