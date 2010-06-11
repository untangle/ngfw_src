/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.security;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * An UVM user.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="u_user", schema="settings")
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

    @Id
    @GeneratedValue
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
    @Column(nullable=false, length=24)
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
    @Column(nullable=false, length=24)
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
    @Column(nullable=false, length=64)
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
    @Column(name="write_access", nullable=false)
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
    @Column(name="reports_access", nullable=false)
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
    @Transient
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
    @Column(length=64)
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
    @Column(length=256)
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
    @Column(name="send_alerts")
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
