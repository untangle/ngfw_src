package com.untangle.app.smtp.safelist;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SafelistCount implements Serializable
{

    String emailAddress;
    int count;

    public SafelistCount() {
    }

    public SafelistCount(String emailAddress, int count) {
        this.emailAddress = emailAddress;
        this.count = count;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public String getEmailAddress()
    {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress)
    {
        this.emailAddress = emailAddress;
    }

}
