/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.untangle.uvm.Period;

/**
 * Uvm administrator settings.
 */
@SuppressWarnings("serial")
public class AdminSettings implements Serializable
{

    private Long id;
    private Set<User> users = new HashSet<User>();
    private Period summaryPeriod;

    public AdminSettings() { }

    public Long getId() { return id; }
    public void setId( Long id ) { this.id = id; }

    /**
     * Specifies a set of system administrators with login access to
     * the system.
     */
    public Set<User> getUsers() { return users; }
    public void setUsers( Set<User > users) { this.users = users; }

    public void addUser(User user)
    {
        users.add(user);
    }

    /**
     * Specifies how often summary alerts/reports are generated.
     */
    public Period getSummaryPeriod() { return summaryPeriod; }
    public void setSummaryPeriod( Period summaryPeriod ) { this.summaryPeriod = summaryPeriod; }
}
