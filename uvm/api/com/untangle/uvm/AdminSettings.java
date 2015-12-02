/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Uvm administrator settings.
 */
@SuppressWarnings("serial")
public class AdminSettings implements Serializable, JSONString
{
    private LinkedList<AdminUserSettings> users = new LinkedList<AdminUserSettings>();
    private Long version = null;

    public AdminSettings() { }

    /**
     * Specifies a set of system administrators with login access to the system
     */
    public LinkedList<AdminUserSettings> getUsers() { return users; }
    public void setUsers( LinkedList<AdminUserSettings> users ) { this.users = users; }

    public Long getVersion() { return version; }
    public void setVersion( Long newValue ) { this.version = newValue; }
    
    public void addUser(AdminUserSettings user)
    {
        this.users.add(user);
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
