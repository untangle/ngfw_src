/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

import java.io.Serializable;

/**
 * Manage safelist count.
 */
@SuppressWarnings("serial")
public class SafelistCount implements Serializable
{

    String emailAddress;
    int count;

    /**
     * Initiaize safelist count.
     */
    public SafelistCount() {
    }

    /**
     * Initialize safelist count with email address and count.
     *
     * @param  emailAddress Email address of safelist owner.
     * @param  count        Count if entries in safelist.
     */
    public SafelistCount(String emailAddress, int count) {
        this.emailAddress = emailAddress;
        this.count = count;
    }

    /**
     * Return safelist count.
     * 
     * @return Count of entries in safelist.
     */
    public int getCount()
    {
        return count;
    }

    /**
     * Specify count of entries in safelist.
     * 
     * @param count Integer count of entries in safelist.
     */
    public void setCount(int count)
    {
        this.count = count;
    }

    /**
     * Get email address of safelist owner.
     * 
     * @return String of email address of safelist owner.
     */
    public String getEmailAddress()
    {
        return emailAddress;
    }

    /**
     * Specify email address of safelist owner.
     * @param emailAddress String of safelist owner email address.
     */
    public void setEmailAddress(String emailAddress)
    {
        this.emailAddress = emailAddress;
    }

}
