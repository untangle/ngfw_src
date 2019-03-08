/**
 * $Id$
 */

package com.untangle.app.spam_blocker;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Spam control: Definition of spam control settings.
 */
@SuppressWarnings("serial")
public class SpamSmtpConfig implements Serializable, JSONString
{
    public static final int DEFAULT_MESSAGE_SIZE_LIMIT = 1 << 20;
    public static final int DEFAULT_STRENGTH = 43;
    public static final boolean DEFAULT_ADD_SPAM_HEADERS = false;
    public static final boolean DEFAULT_SCAN = false;
    public static final String DEFAULT_HEADER_NAME = "X-Spam-Flag";

    public static final int DEFAULT_SUPER_STRENGTH = 200;
    public static final boolean DEFAULT_GREYLIST = false;
    public static final boolean DEFAULT_TARPIT = false;
    public static final int DEFAULT_TARPIT_TIMEOUT = 15;
    public static final boolean DEFAULT_FAIL_CLOSED = true;
    public static final boolean DEFAULT_BLOCK_SUPER_SPAM = true;
    public static final float DEFAULT_LIMIT_LOAD = 7.0f;
    public static final int DEFAULT_LIMIT_SCANS = 15;
    public static final boolean DEFAULT_SCAN_WAN_MAIL = false;
    public static final boolean DEFAULT_ALLOW_TLS = false;

    // // Help for the UI follows.
    // public static final int LOW_STRENGTH = 50;
    // public static final int MEDIUM_STRENGTH = 43;
    // public static final int HIGH_STRENGTH = 35;
    // public static final int VERY_HIGH_STRENGTH = 33;
    // public static final int EXTREME_STRENGTH = 30;
    
    /* settings */
    private boolean bScan = DEFAULT_SCAN;
    private int strength = DEFAULT_STRENGTH;
    private boolean addSpamHeaders = DEFAULT_ADD_SPAM_HEADERS;
    private int msgSizeLimit = DEFAULT_MESSAGE_SIZE_LIMIT;
    private String headerName = DEFAULT_HEADER_NAME;
    private boolean tarpit = DEFAULT_TARPIT;
    private boolean greylist = DEFAULT_GREYLIST;
    private int tarpit_timeout = DEFAULT_TARPIT_TIMEOUT;
    private SpamMessageAction msgAction = SpamMessageAction.QUARANTINE;
    private int superSpamStrength = DEFAULT_SUPER_STRENGTH;
    private boolean blockSuperSpam = DEFAULT_BLOCK_SUPER_SPAM;
    private boolean failClosed = DEFAULT_FAIL_CLOSED;
    private float limit_load = DEFAULT_LIMIT_LOAD;
    private int limit_scans = DEFAULT_LIMIT_SCANS;
    private boolean scan_wan_mail = DEFAULT_SCAN_WAN_MAIL;
    private boolean allowTls = DEFAULT_ALLOW_TLS;
    
    public SpamSmtpConfig() { }

    public SpamSmtpConfig(boolean bScan,
                          SpamMessageAction msgAction,
                          int strength,
                          boolean addSpamHeaders,
                          boolean blockSuperSpam,
                          int superSpamStrength,
                          boolean failClosed,
                          String headerName,
                          boolean tarpit,
                          int tarpit_timeout,
                          float limit_load,
                          int limit_scans,
                          boolean scan_wan_mail,
                          boolean allowTls)
    {
        this.bScan = bScan;
        this.strength = strength;
        this.addSpamHeaders = addSpamHeaders;
        this.headerName = headerName;
        this.blockSuperSpam = blockSuperSpam;
        this.superSpamStrength = superSpamStrength;
        this.failClosed = failClosed;
        this.msgAction = msgAction;
        this.tarpit = tarpit;
        this.tarpit_timeout = tarpit_timeout;
        this.limit_load = limit_load;
        this.limit_scans = limit_scans;
        this.scan_wan_mail = scan_wan_mail;
        this.allowTls = allowTls;
    }

    /*
     * Get the name of the header (e.g. "X-SPAM") used to indicate the
     * SPAM/HAM value of this email
     */
    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /*
     * scan: a boolean specifying whether or not to scan a message for
     * spam (defaults to true)
     *
     * @return whether or not to scan message for spam
     */
    public boolean getScan() { return bScan; }
    public void setScan( boolean newValue ) { this.bScan = newValue; }

    /*
     * strength: an integer giving scan strength.  Divide by 10 to get
     * SpamAssassin strength.  Thus range should be something like: 30
     * to 100
     *
     * @return an <code>int</code> giving the spam strength * 10
     */
    public int getStrength() { return strength; }
    public void setStrength( int newValue ) { this.strength = newValue; }

    public boolean getAddSpamHeaders() { return addSpamHeaders; }
    public void setAddSpamHeaders( boolean newValue ) { this.addSpamHeaders = newValue; }

    /*
     * msgSizeLimit: an integer giving scan message size limit.  Files
     * over this size are presumed not to be spam, and not scanned for
     * performance reasons.
     *
     * @return an <code>int</code> giving the spam message size limit
     * (cutoff) in bytes.
     */
    public int getMsgSizeLimit() { return msgSizeLimit; }
    public void setMsgSizeLimit( int newValue ) { this.msgSizeLimit = newValue; }

    public boolean getBlockSuperSpam() { return blockSuperSpam; }
    public void setBlockSuperSpam( boolean newValue ) { this.blockSuperSpam = newValue; }

    public int getSuperSpamStrength() { return superSpamStrength; }
    public void setSuperSpamStrength(int newValue) { this.superSpamStrength = newValue; }

    public boolean getFailClosed() { return failClosed; }
    public void setFailClosed( boolean newValue ) { this.failClosed = newValue; }

    /*
     * messageAction: an action specifying a response if a message
     * contains spam 
     *
     * @return the action to take if a message is judged to be spam.
     */
    public SpamMessageAction getMsgAction() { return msgAction; }
    public void setMsgAction( SpamMessageAction newValue ) { this.msgAction = newValue; }

    /*
     * greylist: a boolean specifying whether or not to greylist
     *
     * @return whether or not to reject a spammer
     */
    public boolean getGreylist() { return greylist; }
    public void setGreylist( boolean newValue ) { this.greylist = newValue; }

    /*
     * tarpit: a boolean specifying whether or not to reject a
     * connection from a suspect spammer
     *
     * @return whether or not to reject a spammer
     */
    public boolean getTarpit() { return tarpit; }
    public void setTarpit( boolean newValue ) { this.tarpit = newValue; }

    /*
     * tarpit_timeout: a timeout in seconds for a tarpit
     * DNSBL lookup
     *
     * @return timeout in seconds for tarpit lookups
     */
    public int getTarpitTimeout() { return tarpit_timeout; }
    public void setTarpitTimeout(int newValue) { this.tarpit_timeout = newValue; }

    /*
     * limit_scans: Limit for simultaneous scans
     * When the scan count is over this new scans will be rejected
     *
     * @return timeout in seconds for tarpit lookups
     */
    public int getScanLimit() { return limit_scans; }
    public void setScanLimit( int newValue ) { this.limit_scans = newValue; }

    /*
     * limit_load: Limit for scanning load
     * When the load is over this new scans will be rejected
     *
     * @return timeout in seconds for tarpit lookups
     */
    public float getLoadLimit() { return limit_load; }
    public void setLoadLimit( float newValue ) { this.limit_load = newValue; }

    /*
     * scan_wan_mail: a boolean specifying whether or not to scan 
     * smtp going out a WAN interface
     *
     * @return boolean value
     */
    public boolean getScanWanMail() { return scan_wan_mail; }
    public void setScanWanMail( boolean newValue ) { this.scan_wan_mail = newValue; }

    /*
     * allowTls: a boolean specifying whether or not to allow TLS sessions to bypass
     * scanning. if false, TLS is blocked. if true, TLS is allowed and will be unscanned
     *
     * @return boolean value
     */
    public boolean getAllowTls() { return allowTls; }
    public void setAllowTls( boolean newValue ) { this.allowTls = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
