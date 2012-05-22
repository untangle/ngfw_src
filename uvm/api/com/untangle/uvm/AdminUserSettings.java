/**
 * $Id: AdminUserSettings.java,v 1.00 2012/05/14 12:58:47 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.commons.codec.binary.Base64;

import com.untangle.uvm.PasswordUtil;

@SuppressWarnings("serial")
public class AdminUserSettings implements Serializable, JSONString
{
    private String username;
    private String emailAddress;
    private String description;
    private byte[] passwordHash = null;

    public AdminUserSettings() {}

    public AdminUserSettings( String username, String password, String description)
    {
        setUsername( username );
        setPassword( password );
        setDescription( description );
        this.emailAddress = null;
    }
    
    public String getUsername() { return this.username; }
    public void setUsername( String username ) { this.username = username; }

    public String getEmailAddress() { return this.emailAddress; }
    public void setEmailAddress( String emailAddress ) { this.emailAddress = emailAddress; }
    
    public String getDescription() { return this.description; }
    public void setDescription( String description ) { this.description = description; }
    
    public byte[] trans_getPasswordHash()
    {
        return this.passwordHash;
    }

    public void setPasswordHash( byte[] passwordHash )
    {
        this.passwordHash = passwordHash;
    }

    public String getPassword()
    {
        return null;
    }

    public void setPassword( String password )
    {
        if ( password == null )
            return;
        if ( "".equals(password) )
            return;
        this.passwordHash = PasswordUtil.encrypt(password);
    }

    public String getPasswordHashBase64()
    {
        if ( this.passwordHash == null || "".equals(this.passwordHash) )
            return "";
        else
            return new String(Base64.encodeBase64(passwordHash));
    }

    public void setPasswordHashBase64( String passwordHashBase64 )
    {
        if ( passwordHashBase64 == null )
            return;
        if ( "".equals( passwordHashBase64 ) )
            return;
        this.passwordHash = Base64.decodeBase64(passwordHashBase64.getBytes());
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}

    