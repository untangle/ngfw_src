/**
 * $Id$
 */

package com.untangle.uvm.app;

import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * The License class
 */
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
    public static final String BANDWIDTH_CONTROL = "bandwidth-control";
    public static final String CONFIGURATION_BACKUP = "configuration-backup";
    public static final String BRANDING_MANAGER = "branding-manager";
    public static final String VIRUS_BLOCKER = "virus-blocker";
    public static final String SPAM_BLOCKER = "spam-blocker";
    public static final String WAN_FAILOVER = "wan-failover";
    public static final String IPSEC_VPN = "ipsec-vpn";
    public static final String POLICY_MANAGER = "policy-manager";
    public static final String WEB_FILTER = "web-filter";
    public static final String WAN_BALANCER = "wan-balancer";
    public static final String WEB_CACHE = "web-cache";
    public static final String APPLICATION_CONTROL = "application-control";
    public static final String SSL_INSPECTOR = "ssl-inspector";
    public static final String LIVE_SUPPORT = "live-support";

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

    /** The string to display */
    private String seatsDisplay = null;

    /** This stores the computed validity state of this license */
    private Boolean valid;

    /** This stores the human-readable form of the status */
    private String status;

    /**
     * Constructor
     */
    public License()
    {
        this.valid = Boolean.FALSE;
    }

    /**
     * Constructor
     * 
     * @param orig
     *        The license to use for initialization
     */
    public License(License orig)
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
        this.seatsDisplay = orig.seatsDisplay;
    }

    /**
     * Constructor
     * 
     * @param name
     *        The application name
     * @param uid
     *        The UID
     * @param displayName
     *        The display name
     * @param type
     *        THe t ype
     * @param start
     *        The start
     * @param end
     *        The end
     * @param key
     *        The key
     * @param keyVersion
     *        The key version
     * @param valid
     *        The valid folag
     * @param status
     *        The status
     */
    public License(String name, String uid, String displayName, String type, long start, long end, String key, int keyVersion, Boolean valid, String status)
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
     * Returns the unique identifier of the product of this license This is the
     * current "updated" version of the "name" We have changed names several
     * times, but the licenses server still provides licenses for the old names
     * This provides the newest current name for any name
     * 
     * @return The name
     */
    public String getCurrentName()
    {
        //update all old names to the new names
        switch (this.name)
        {
        case "adconnector":
            return "directory-connector";
        case "untangle-node-adconnector":
            return "directory-connector";
        case "untangle-node-directory-connector":
            return "directory-connector";
        case "bandwidth":
            return "bandwidth-control";
        case "untangle-node-bandwidth":
            return "bandwidth-control";
        case "untangle-node-bandwidth-control":
            return "bandwidth-control";
        case "boxbackup":
            return "configuration-backup";
        case "untangle-node-boxbackup":
            return "configuration-backup";
        case "untangle-node-configuration-backup":
            return "configuration-backup";
        case "branding":
            return "branding-manager";
        case "untangle-node-branding":
            return "branding-manager";
        case "untangle-node-branding-manager":
            return "branding-manager";
        case "virusblocker":
            return "virus-blocker";
        case "untangle-node-virusblocker":
            return "virus-blocker";
        case "commtouchav":
            return "virus-blocker";
        case "untangle-node-commtouchav":
            return "virus-blocker";
        case "kav":
            return "virus-blocker";
        case "untangle-node-kav":
            return "virus-blocker";
        case "untangle-node-virus-blocker":
            return "virus-blocker";
        case "spamblocker":
            return "spam-blocker";
        case "untangle-node-spamblocker":
            return "spam-blocker";
        case "commtouchas":
            return "spam-blocker";
        case "untangle-node-commtouchas":
            return "spam-blocker";
        case "untangle-node-spam-blocker":
            return "spam-blocker";
        case "faild":
            return "wan-failover";
        case "untangle-node-faild":
            return "wan-failover";
        case "untangle-node-wan-failover":
            return "wan-failover";
        case "ipsec":
            return "ipsec-vpn";
        case "untangle-node-ipsec":
            return "ipsec-vpn";
        case "untangle-node-ipsec-vpn":
            return "ipsec-vpn";
        case "policy":
            return "policy-manager";
        case "untangle-node-policy":
            return "policy-manager";
        case "untangle-node-policy-manager":
            return "policy-manager";
        case "sitefilter":
            return "web-filter";
        case "untangle-node-sitefilter":
            return "web-filter";
        case "untangle-node-web-filter":
            return "web-filter";
        case "splitd":
            return "wan-balancer";
        case "untangle-node-splitd":
            return "wan-balancer";
        case "untangle-node-wan-balancer":
            return "wan-balancer";
        case "webcache":
            return "web-cache";
        case "untangle-node-webcache":
            return "web-cache";
        case "untangle-node-web-cache":
            return "web-cache";
        case "classd":
            return "application-control";
        case "untangle-node-classd":
            return "application-control";
        case "untangle-node-application-control":
            return "application-control";
        case "https":
            return "ssl-inspector";
        case "untangle-node-https":
            return "ssl-inspector";
        case "untangle-node-ssl-inspector":
            return "ssl-inspector";
        case "untangle-casing-https":
            return "ssl-inspector";
        case "untangle-casing-ssl-inspector":
            return "ssl-inspector";
        case "support":
            return "live-support";
        case "untangle-node-support":
            return "live-support";
        case "untangle-node-live-support":
            return "live-support";
        default:
            break;
        }

        return this.name;
    }

    /**
     * Returns the unique identifier of the product of this license
     * 
     * @return The name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Set the name
     * 
     * @param newValue
     *        The new name
     */
    public void setName(String newValue)
    {
        this.name = newValue;
    }

    /**
     * Returns the unique identifier of the product of this license
     * 
     * @return The UID
     */
    public String getUID()
    {
        return this.uid;
    }

    /**
     * Set the UID
     * 
     * @param newValue
     *        The new UID
     */
    public void setUID(String newValue)
    {
        this.uid = newValue;
    }

    /**
     * Returns the human readable name of the product of this license
     * 
     * @return The display name
     */
    public String getDisplayName()
    {
        return this.displayName;
    }

    /**
     * Set the display name
     * 
     * @param newValue
     *        The new display name
     */
    public void setDisplayName(String newValue)
    {
        this.displayName = newValue;
    }

    /**
     * Get the type of license. This would be used for a descriptive name of the
     * license, like 30 Day Trial, Academic, Professional, etc.
     * 
     * @return The type
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * Set the type
     * 
     * @param newValue
     *        The new type
     */
    public void setType(String newValue)
    {
        this.type = newValue;
    }

    /**
     * Get the end of the license, milliseconds. Stored as a long to insure
     * database timezone changes don't freak it out.
     * 
     * @return The start
     */
    public long getStart()
    {
        return this.start;
    }

    /**
     * Set the start
     * 
     * @param newValue
     *        The new start
     */
    public void setStart(long newValue)
    {
        this.start = newValue;
    }

    /**
     * Get the end of the license, milliseconds. Stored as a long to insure
     * database timezone changes don't freak it out.
     * 
     * @return The end
     */
    public long getEnd()
    {
        return this.end;
    }

    /**
     * Set the end
     * 
     * @param newValue
     *        The new end
     */
    public void setEnd(long newValue)
    {
        this.end = newValue;
    }

    /**
     * Set the key for this license.
     * 
     * @return The key
     */
    public String getKey()
    {
        return this.key;
    }

    /**
     * Set the key
     * 
     * @param newValue
     *        The new key
     */
    public void setKey(String newValue)
    {
        this.key = newValue;
    }

    /**
     * Set the key version for this license.
     * 
     * @return The key version
     */
    public int getKeyVersion()
    {
        return this.keyVersion;
    }

    /**
     * Set the key version
     * 
     * @param newValue
     *        The new key version
     */
    public void setKeyVersion(int newValue)
    {
        this.keyVersion = newValue;
    }

    /**
     * Get the number of seats for the license
     * 
     * @return The number of seats
     */
    public Integer getSeats()
    {
        return this.seats;
    }

    /**
     * Set the seats
     * 
     * @param newValue
     *        The new seats
     */
    public void setSeats(Integer newValue)
    {
        this.seats = newValue;
    }

    /**
     * Get the seats display
     * 
     * @return The seats display
     */
    public String getSeatsDisplay()
    {
        if (this.seatsDisplay != null) return this.seatsDisplay;
        else {
            if (this.seats == null) return null;
            else return this.seats.toString();
        }
    }

    /**
     * Set the seats display
     * 
     * @param newValue
     *        The new display value
     */
    public void setSeatsDisplay(String newValue)
    {
        this.seatsDisplay = newValue;
    }

    /**
     * Returns the valid state This is a transient value - it is set (or reset)
     * on settings load
     * 
     * @return The valid state of the license
     */
    public Boolean getValid()
    {
        if (this.valid == null) return Boolean.FALSE;

        return this.valid;
    }

    /**
     * Set the valid state
     * 
     * @param valid
     *        The new valid state
     */
    public void setValid(Boolean valid)
    {
        this.valid = valid;
    }

    /**
     * Get the status of license. Example: "Valid" "Invalid (Expired)"
     * "Invalid (UID Mismatch)" etc This is a transient value - it is set (or
     * reset) on settings load
     * 
     * @return The status
     */
    public String getStatus()
    {
        return this.status;
    }

    /**
     * Set the status
     * 
     * @param status
     *        The new status
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * Returns true if this is a trial license This is a transient value
     * 
     * @return True if trial license, otherwise false
     */
    public Boolean getTrial()
    {
        if (License.LICENSE_TYPE_TRIAL.equals(this.getType())) return Boolean.TRUE;
        return Boolean.FALSE;
    }

    /**
     * Returns true if this license is expired This is a transient value
     * 
     * @return True if expired, otherwise false
     */
    public Boolean getExpired()
    {
        long now = (System.currentTimeMillis() / 1000);

        if (now > this.getEnd()) return Boolean.TRUE;
        else return Boolean.FALSE;
    }

    /**
     * Returns the number of days remaining until end-date This is a transient
     * value
     * 
     * @return Number of days remaining
     */
    public Integer getDaysRemaining()
    {
        long now = (System.currentTimeMillis() / 1000);
        int days = ((int) (getEnd() - now)) / (60 * 60 * 24);
        return new Integer(days);
    }

    /**
     * Return the string representation
     * 
     * @return The string representation
     */
    public String toString()
    {
        return "<" + this.uid + "/" + this.name + "/" + this.type + "/" + ">";
    }
}
