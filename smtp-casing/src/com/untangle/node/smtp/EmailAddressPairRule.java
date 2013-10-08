/**
 * $Id: EmailAddressPairRule.java 34290 2013-03-17 00:00:19Z dmorris $
 */
package com.untangle.node.smtp;

import java.io.Serializable;

/**
 * Class used to associate two email addresses
 */
@SuppressWarnings("serial")
public class EmailAddressPairRule implements Serializable
{
    private String addr1;
    private String addr2;

    public EmailAddressPairRule() {
        this(null, null);
    }

    public EmailAddressPairRule(String addr1, String addr2) {
        this.addr1 = addr1;
        this.addr2 = addr2;
    }

    public void setAddress1(String addr1)
    {
        this.addr1 = addr1;
    }

    public String getAddress1()
    {
        return this.addr1;
    }

    public void setAddress2(String addr2)
    {
        this.addr2 = addr2;
    }

    public String getAddress2()
    {
        return this.addr2;
    }
}
