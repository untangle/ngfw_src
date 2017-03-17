/*
 * $Id$
 */
package com.untangle.uvm.app;

import java.io.Serializable;
import java.util.Date;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class License implements Serializable
{
    private final Logger logger = Logger.getLogger(License.class);

    public static final String LICENSE_TYPE_TRIAL = "Trial";
    public static final String LICENSE_TYPE_SUBSCRIPTION = "Subscription";
    
    /**
     * Various Names
     */
    public static final String DIRECTORY_CONNECTOR = "directory-connector";
    public static final String DIRECTORY_CONNECTOR_OLDNAME = "adconnector";
    public static final String BANDWIDTH_CONTROL = "bandwidth-control";
    public static final String BANDWIDTH_CONTROL_OLDNAME = "bandwidth";
    public static final String CONFIGURATION_BACKUP = "configuration-backup";
    public static final String CONFIGURATION_BACKUP_OLDNAME = "boxbackup";
    public static final String BRANDING_MANAGER = "branding-manager";
    public static final String BRANDING_MANAGER_OLDNAME = "branding";
    public static final String VIRUS_BLOCKER = "virus-blocker";
    public static final String VIRUS_BLOCKER_OLDNAME = "virusblocker";
    public static final String SPAM_BLOCKER = "spam-blocker";
    public static final String SPAM_BLOCKER_OLDNAME = "spamblocker";
    public static final String WAN_FAILOVER = "wan-failover";
    public static final String WAN_FAILOVER_OLDNAME = "faild";
    public static final String IPSEC_VPN = "ipsec-vpn";
    public static final String IPSEC_VPN_OLDNAME = "ipsec";
    public static final String POLICY_MANAGER = "policy-manager";
    public static final String POLICY_MANAGER_OLDNAME = "policy";
    public static final String WEB_FILTER = "web-filter";
    public static final String WEB_FILTER_OLDNAME = "sitefilter";
    public static final String WAN_BALANCER = "wan-balancer";
    public static final String WAN_BALANCER_OLDNAME = "splitd";
    public static final String WEB_CACHE = "web-cache";
    public static final String WEB_CACHE_OLDNAME = "webcache";
    public static final String APPLICATION_CONTROL = "application-control";
    public static final String APPLICATION_CONTROL_OLDNAME = "classd";
    public static final String SSL_INSPECTOR = "ssl-inspector";
    public static final String SSL_INSPECTOR_OLDNAME = "https";
    public static final String LIVE_SUPPORT = "live-support";
    public static final String LIVE_SUPPORT_OLDNAME = "support";
    
    /** Identifier for the product this license is for */
    private String name;

    /** The UID for this license */
    private String uid;
    
    /** The human readable name */
    private String displayName;
    
    /** This is the type of license */
    private String type;

    /** Start date for the license */
    private long start;
    
    /** End date for the license */
    private long end;

    /** Key for the license */
    private String key;

    /** the version of the key signing algorithm used */
    private int keyVersion;

    /** The licensed number of seats */
    private Integer seats = null;

    /** This stores the computed validity state of this license */
    private Boolean valid;

    /** This stores the human-readable form of the status */
    private String status;

    public License()
    {
        this.valid = Boolean.FALSE;
    }

    public License( License orig )
    {
        this.name = orig.name;
        this.uid = orig.uid;
        this.displayName = orig.displayName;
        this.type = orig.type;
        this.start = orig.start;
        this.end = orig.end;
        this.key = orig.key;
        this.keyVersion = orig.keyVersion;
        this.valid = orig.valid;
        this.status = orig.status;
        this.seats = orig.seats;
    }
    
    public License( String name, String uid, String displayName, String type, long start, long end, String key, int keyVersion, Boolean valid, String status )
    {
        this.name = name;
        this.uid = uid;
        this.displayName = displayName;
        this.type = type;
        this.start = start;
        this.end = end;
        this.key = key;
        this.keyVersion = keyVersion;
        this.valid = valid;
        this.status = status;
    }
        
    /**
     * Returns the unique identifier of the product of this license
     */
    public String getName() { return this.name; }
    public void setName( String newValue ) { this.name = newValue; }

    /**
     * Returns the unique identifier of the product of this license
     */
    public String getUID() { return this.uid; }
    public void setUID( String newValue ) { this.uid = newValue; }
    
    /**
     * Returns the human readable name of the product of this license
     */
    public String getDisplayName() { return this.displayName; }
    public void setDisplayName( String newValue ) { this.displayName = newValue; }

    /**
     * Get the type of license.  This would be used for a
     * descriptive name of the license, like 30 Day Trial, Academic,
     * Professional, etc.
     */
    public String getType() { return this.type; }
    public void setType( String newValue ) { this.type = newValue; }
    
    /**
     * Get the end of the license, milliseconds.  Stored as a long to
     * insure database timezone changes don't freak it out.
     */
    public long getStart() { return this.start; }
    public void setStart( long newValue ) { this.start = newValue; }
    
    /**
     * Get the end of the license, milliseconds.  Stored as a long to
     * insure database timezone changes don't freak it out.
     */
    public long getEnd() { return this.end; }
    public void setEnd( long newValue ) { this.end = newValue; }
    
    /**
     * Set the key for this license.
     */
    public String getKey() { return this.key; }
    public void setKey( String newValue ) { this.key = newValue; }
    
    /**
     * Set the key version for this license.
     */
    public int getKeyVersion() { return this.keyVersion; }
    public void setKeyVersion( int newValue ) { this.keyVersion = newValue; }

    /**
     * Set the key version for this license.
     */
    public Integer getSeats() { return this.seats; }
    public void setSeats( Integer newValue ) { this.seats = newValue; }

    /**
     * Returns the valid state
     * This is a transient value - it is set (or reset) on settings load
     */
    public Boolean getValid()
    {
        if (this.valid == null)
            return Boolean.FALSE;

        return this.valid;
    }

    public void setValid( Boolean valid )
    {
        this.valid = valid;
    }

    /**
     * Get the status of license.
     * Example: "Valid" "Invalid (Expired)" "Invalid (UID Mismatch)" etc
     * This is a transient value - it is set (or reset) on settings load
     */
    public String getStatus()
    {
        return this.status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    /**
     * Returns true if this is a trial license
     * This is a transient value
     */
    public Boolean getTrial()
    {
        if (License.LICENSE_TYPE_TRIAL.equals(this.getType()))
            return Boolean.TRUE;
        return Boolean.FALSE;
    }
    
    /**
     * Returns true if this license is expired
     * This is a transient value
     */
    public Boolean getExpired()
    {
        long now = (System.currentTimeMillis()/1000);

        if (now > this.getEnd())
            return Boolean.TRUE;
        else
            return Boolean.FALSE;
    }
    
    /**
     * Returns the number of days remaining until end-date
     * This is a transient value
     */
    public Integer getDaysRemaining()
    {
        long now = (System.currentTimeMillis()/1000);
        int days = ((int)(getEnd() - now)) / (60*60*24);
        return new Integer(days);
    }
    
    public String toString()
    {
        return "<" + this.uid + "/" + this.name + "/" + this.type + "/" + ">";
    }

}
