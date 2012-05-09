/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

/**
 * An UVM user.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class User implements Serializable
{

    private Long id;
    private String login;
    private byte[] password;
    private String name  = "[no name]";
    private String email = "[no email]";
    private String notes = "[no description]";
    private boolean sendAlerts = false;
    private boolean hasWriteAccess = true;
    private boolean hasReportsAccess = true;

    public User() { }

    /**
     * Creates a new <code>User</code> instance.
     *
     * @param login user login
     * @param password in cleartext.
     * @param name human name.
     */
    public User(String login, String password, String name)
    {
        this.login = login;
        this.password = PasswordUtil.encrypt(password);
        this.name = name;
    }

    /**
     * Creates a new <code>User</code> instance.
     *
     * @param login user login
     * @param password hashed.
     * @param name human name.
     */
    public User(String login, byte[] password, String name)
    {
        this.login = login;
        this.password = password;
        this.name = name;
    }

    /**
     * Creates a new <code>User</code> instance.
     *
     * @param login user login
     * @param password in cleartext;
     * @param name human name.
     * @param email email address for alerts.
     * @param notes notes about user.
     * @param sendAlerts true if user should get alerts.
     */
    public User(String login, String password, String name,
                String email, String notes, boolean sendAlerts)
    {
        this.login = login;
        this.name = name;
        this.password = PasswordUtil.encrypt(password);
        this.email = email;
        this.notes = notes;
        this.sendAlerts = sendAlerts;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Login name.
     *
     * @return user login.
     */
    public String getLogin()
    {
        return login;
    }

    public void setLogin(String login)
    {
        this.login = login;
    }

    /**
     * Password, encrypted with password utils.
     *
     * @return encrypted password bytes.
     */
    public byte[] getPassword()
    {
        return password;
    }

    /* for hibernate only */
	private void setPassword(byte[] password)
    {
        this.password = password;
    }

    /**
     * Name.
     *
     * @return username.
     */
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * True if this user can modify settings.
     *
     * @return true True if this user can modify settings.
     */
    public boolean getHasWriteAccess()
    {
        return hasWriteAccess;
    }

    public void setHasWriteAccess(boolean newValue)
    {
        this.hasWriteAccess = newValue;
    }


    /**
     * True if this user can view reports.
     *
     * @return true if this user can view reports.
     */
    public boolean getHasReportsAccess()
    {
        return hasReportsAccess;
    }

    public void setHasReportsAccess(boolean newValue)
    {
        this.hasReportsAccess = newValue;
    }

    /**
     * Set password from a clear string.
     *
     * @param password to be encrypted.
     */
    public void setClearPassword(String password)
    {
        if (password == null) {
            this.password = null;
        } else {
            this.password = PasswordUtil.encrypt(password);
        }
    }

    /**
     * Set email for UVM to send messages.
     *
     * @return user's contact email.
     */
    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * GECOS field.
     *
     * @return random info about user.
     */
    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    /**
     * Specifies if this user will receive email Alerts.
     *
     * @return true if alerts are sent.
     */
    public boolean getSendAlerts()
    {
        return sendAlerts;
    }

    public void setSendAlerts(boolean sendAlerts)
    {
        this.sendAlerts = sendAlerts;
    }

    // Object methods ---------------------------------------------------------

    /**
     * Equality on the business key is User: (login).
     *
     * @param o object to compare with.
     * @return true if business key equal, false otherwise.
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof User)) {
            return false;
        }

        User u = (User)o;
        return login.equals(u.getLogin());
    }

    public int hashCode()
    {
        return login.hashCode();
    }

    public String toString()
    {
        return "User [ id = " + id + " login = " + login
            + " password = " + password + " name = " + name
            + " email = " + email + " notes = " + notes
            + " alerts = " + sendAlerts + " ]";
    }

    public void updatePassword(User user) {
        this.password = user.password;
    }

}
