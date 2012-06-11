/**
 * $Id: TableOfContents.java,v 1.00 2012/06/11 14:57:55 dmorris Exp $
 */
package com.untangle.uvm.reports;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
public class TableOfContents implements Serializable
{
    private final Application platform;

    private final List<Application> applications;

    private final Date date;

    private final String user;
    private final String host;
    private final String email;

    private final List<User> users;
    private final List<Host> hosts;
    private final List<Email> emails;

    public TableOfContents(Date date, String user, String host, String email,
                           Application platform, List<Application> applications,
                           List<User> users, List<Host> hosts,
                           List<Email> emails)
    {
        this.date = date;

        this.user = user;
        this.host = host;
        this.email = email;

        this.platform = platform;
        this.applications = applications;
        this.users = users;
        this.hosts = hosts;
        this.emails = emails;
    }

    public Date getDate()
    {
        return date;
    }

    public String getUser()
    {
        return user;
    }

    public String getHost()
    {
        return host;
    }

    public String getEmail()
    {
        return email;
    }

    public Application getPlatform()
    {
        return platform;
    }

    public List<Application> getApplications()
    {
        return applications;
    }

    public List<User> getUsers()
    {
        return users;
    }

    public List<Host> getHosts()
    {
        return hosts;
    }

    public List<Email> getEmails()
    {
        return emails;
    }
}