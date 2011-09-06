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

package com.untangle.node.spam;

/**
 * Spam control: Definition of spam control settings (either direction)
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class SpamSmtpConfig extends SpamProtoConfig
{
    public static final int DEFAULT_SUPER_STRENGTH = 200;
    public static final boolean DEFAULT_TARPIT = false;
    public static final int DEFAULT_TARPIT_TIMEOUT = 15;
    public static final boolean DEFAULT_FAIL_CLOSED = true;
    public static final boolean DEFAULT_BLOCK_SUPER_SPAM = true;
    public static final float DEFAULT_LIMIT_LOAD = 7.0f;
    public static final int DEFAULT_LIMIT_SCANS = 15;
    public static final boolean DEFAULT_SCAN_WAN_MAIL = false;

    /* settings */
    private boolean tarpit = DEFAULT_TARPIT;
    private int tarpit_timeout = DEFAULT_TARPIT_TIMEOUT;
    private SmtpSpamMessageAction msgAction = SmtpSpamMessageAction.QUARANTINE;
    private int superSpamStrength = DEFAULT_SUPER_STRENGTH;
    private boolean blockSuperSpam = DEFAULT_BLOCK_SUPER_SPAM;
    private boolean failClosed = DEFAULT_FAIL_CLOSED;
    private float limit_load = DEFAULT_LIMIT_LOAD;
    private int limit_scans = DEFAULT_LIMIT_SCANS;
    private boolean scan_wan_mail = DEFAULT_SCAN_WAN_MAIL;

    public SpamSmtpConfig() { }

    public SpamSmtpConfig(boolean bScan,
                          SmtpSpamMessageAction msgAction,
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
                          boolean scan_wan_mail)
    {
        super(bScan,
              strength,
              addSpamHeaders,
              headerName);
        this.blockSuperSpam = blockSuperSpam;
        this.superSpamStrength = superSpamStrength;
        this.failClosed = failClosed;
        this.msgAction = msgAction;
        this.tarpit = tarpit;
        this.tarpit_timeout = tarpit_timeout;
        this.scan_wan_mail = scan_wan_mail;
        this.limit_load = limit_load;
        this.limit_scans = limit_scans;
    }

    // accessors --------------------------------------------------------------


    /**
     * Get the tempalte used to create the subject
     * for a notification message.
     */

    public boolean getBlockSuperSpam()
    {
        return blockSuperSpam;
    }

    public void setBlockSuperSpam(boolean blockSuperSpam)
    {
        this.blockSuperSpam = blockSuperSpam;
    }

    public int getSuperSpamStrength()
    {
        return superSpamStrength;
    }

    public void setSuperSpamStrength(int superSpamStrength)
    {
        this.superSpamStrength = superSpamStrength;
    }

    public boolean getFailClosed()
    {
        return failClosed;
    }

    public void setFailClosed(boolean failClosed)
    {
        this.failClosed = failClosed;
    }

    /**
     * messageAction: a string specifying a response if a message
     * contains spam (defaults to MARK) one of BLOCK, MARK, or PASS
     *
     * @return the action to take if a message is judged to be spam.
     */
    public SmtpSpamMessageAction getMsgAction()
    {
        return msgAction;
    }

    public void setMsgAction(SmtpSpamMessageAction msgAction)
    {
        this.msgAction = msgAction;
    }

    /**
     * tarpit: a boolean specifying whether or not to reject a
     * connection from a suspect spammer
     *
     * @return whether or not to reject a spammer
     */
    public boolean getTarpit()
    {
        return tarpit;
    }

    public void setTarpit(boolean tarpit)
    {
        this.tarpit = tarpit;
    }

    /**
     * tarpit_timeout: a timeout in seconds for a tarpit
     * RBL lookup
     *
     * @return timeout in seconds for tarpit lookups
     */
    public int getTarpitTimeout()
    {
        return tarpit_timeout;
    }

    public void setTarpitTimeout(int tarpit_timeout)
    {
        this.tarpit_timeout = tarpit_timeout;
        return;
    }

    /**
     * limit_scans: Limit for simultaneous scans
     * When the scan count is over this new scans will be rejected
     *
     * @return timeout in seconds for tarpit lookups
     */
    public int getScanLimit()
    {
        return limit_scans;
    }

    public void setScanLimit(int limit_scans)
    {
        this.limit_scans = limit_scans;
    }

    /**
     * limit_load: Limit for scanning load
     * When the load is over this new scans will be rejected
     *
     * @return timeout in seconds for tarpit lookups
     */
    public float getLoadLimit()
    {
        return limit_load;
    }

    public void setLoadLimit(float limit_load)
    {
        this.limit_load = limit_load;
    }

    /**
     * scan_wan_mail: a boolean specifying whether or not to scan 
     * smtp going out a WAN interface
     *
     * @return boolean value
     */
    public boolean getScanWanMail()
    {
        return scan_wan_mail;
    }

    public void setScanWanMail(boolean scan_wan_mail)
    {
        this.scan_wan_mail = scan_wan_mail;
    }
}
