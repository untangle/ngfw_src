/*
 * $Id: License.java,v 1.00 2011/08/24 10:48:12 dmorris Exp $
 */
package com.untangle.uvm.node;

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
    public static final String ADCONNECTOR = "untangle-node-adconnector";
    public static final String BANDWIDTH = "untangle-node-bandwidth";
    public static final String BOXBACKUP = "untangle-node-boxbackup";
    public static final String BRANDING = "untangle-node-branding";
    public static final String COMMTOUCH = "untangle-node-commtouch";
    public static final String COMMTOUCHAV = "untangle-node-commtouchav";
    public static final String COMMTOUCHAS = "untangle-node-commtouchas";
    public static final String DATAVAULT = "untangle-node-datavault";
    public static final String FAILD = "untangle-node-faild";
    public static final String IPSEC = "untangle-node-ipsec";
    public static final String POLICY = "untangle-node-policy";
    public static final String SITEFILTER = "untangle-node-sitefilter";
    public static final String SPLITD = "untangle-node-splitd";
    public static final String WEBCACHE = "untangle-node-webcache";
    public static final String CLASSD = "untangle-node-classd";
    
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
    public String getName()
    {
        return this.name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Returns the unique identifier of the product of this license
     */
    public String getUID()
    {
        return this.uid;
    }
    
    public void setUID( String uid )
    {
        this.uid = uid;
    }
    
    /**
     * Returns the human readable name of the product of this license
     */
    public String getDisplayName()
    {
        return this.displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    /**
     * Get the type of license.  This would be used for a
     * descriptive name of the license, like 30 Day Trial, Academic,
     * Professional, etc.
     */
    public String getType()
    {
        return this.type;
    }

    public void setType( String type )
    {
        this.type = type;
    }
    
    /**
     * Get the end of the license, milliseconds.  Stored as a long to
     * insure database timezone changes don't freak it out.
     */
    public long getStart()
    {
        return this.start;
    }

    public void setStart( long start )
    {
        this.start = start;
    }
    
    /**
     * Get the end of the license, milliseconds.  Stored as a long to
     * insure database timezone changes don't freak it out.
     */
    public long getEnd()
    {
        return this.end;
    }

    public void setEnd( long end )
    {
        this.end = end;
    }
    
    /**
     * Set the key for this license.
     */
    public String getKey()
    {
        return this.key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }
    
    /**
     * Set the key version for this license.
     */
    public int getKeyVersion()
    {
        return this.keyVersion;
    }

    public void setKeyVersion( int keyVersion )
    {
        this.keyVersion = keyVersion;
    }
        
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
        if (this.LICENSE_TYPE_TRIAL.equals(this.getType()))
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
