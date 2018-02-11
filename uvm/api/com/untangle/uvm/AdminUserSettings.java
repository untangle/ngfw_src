/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.commons.codec.binary.Base64;

import com.untangle.uvm.PasswordUtil;

/**
 * Uvm administrator user settings.
 */
@SuppressWarnings("serial")
public class AdminUserSettings implements Serializable, JSONString
{
    private String username;
    private String emailAddress;
    private boolean emailAlerts = true;
    private boolean emailSummaries = true;
    private String description;
    private String password;
    private byte[] passwordHash = null;
    private String passwordHashShadow = null;
    
    public AdminUserSettings() {}

    public AdminUserSettings( String username, String password, String description, String emailAddress )
    {
        setUsername( username );
        setPassword( password );
        setDescription( description );
        setEmailAddress( emailAddress );
    }
    
    public String getUsername() { return this.username; }
    public void setUsername( String newValue ) { this.username = newValue; }

    public String getEmailAddress() { return this.emailAddress; }
    public void setEmailAddress( String newValue ) { this.emailAddress = newValue; }

    public boolean getEmailAlerts() { return this.emailAlerts; }
    public void setEmailAlerts( boolean newValue ) { this.emailAlerts = newValue; }

    public boolean getEmailSummaries() { return this.emailSummaries; }
    public void setEmailSummaries( boolean newValue ) { this.emailSummaries = newValue; }
    
    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public String getPasswordHashShadow() { return this.passwordHashShadow; }
    public void setPasswordHashShadow( String newValue ) { this.passwordHashShadow = newValue; }
    
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
        this.password = password;
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

        // only set it if it hasn't been sen by setPassword
        // if its been set already by setPassword, use that value
        if ( this.passwordHash == null ) {
            this.passwordHash = Base64.decodeBase64(passwordHashBase64.getBytes());
        }
    }

    protected String trans_getPassword( )
    {
        return this.password;
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}

    
