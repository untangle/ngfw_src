/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: CTLDefinition.java,v 1.9 2005/03/16 04:00:03 cng Exp $
 */

package com.metavize.tran.email;

import java.io.Serializable;

/**
 * Control information (main settings of email transform)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_CTL_DEFINITION"
 */
public class CTLDefinition implements Serializable
{
    private static final long serialVersionUID = -7772380049816357414L;

    private Long id;

    public static final int MSG_SZ_LIMIT_MAX = 10485760;
    public static final int MSG_SZ_LIMIT_MIN = 2048;

    public static final String NO_DETAILS = "no description";

    /* settings */
    private SScanner spamScanner = SScanner.SPAMAS;
    private VScanner virusScanner = VScanner.NOAV;
    private String pop3Postmaster = "POP3 Postmaster <postmaster@localhost>";
    private String imap4Postmaster = "IMAP4 Postmaster <postmaster@localhost>";
    private boolean copyOnException = false;
    private int msgSzLimit = 8388608;
    private int spamMsgSzLimit = 262144;
    private int virusMsgSzLimit = MSG_SZ_LIMIT_MAX;
    private String imap4PostmasterDetails = NO_DETAILS;
    private String pop3PostmasterDetails = NO_DETAILS;
    private String msgSzLimitDetails = NO_DETAILS;
    private String spamSzLimitDetails = NO_DETAILS;
    private String virusSzLimitDetails = NO_DETAILS;
    private String alertsDetails = NO_DETAILS;
    private String logDetails = NO_DETAILS;
    private boolean returnErrOnSMTPBlock = false;
    private boolean returnErrOnPOP3Block = false;
    private boolean returnErrOnIMAP4Block = false;
    // Not yet:
    // private String senderBlockMessage = "No sender block message";
    // private String receiverBlockMessage = "No receiver block message";
    // private Alerts alertsOnParseException = new Alerts();

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public CTLDefinition() { }

    /*
    public CTLDefinition(String header, String contact)
    {
        this.header = header;
        this.contact = contact;
    }

    // business methods ------------------------------------------------------

    public String render(String site, String category)
    {
        String message = BLOCK_TEMPLATE.replace("@HEADER@", header);
        message = message.replace("@SITE@", site);
        message = message.replace("@CATEGORY@", category);
        message = message.replace("@CONTACT@", contact);

        return message;
    }
    */

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="DEF_ID"
     * generator-class="native"
     */
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * - spamScanner: a string specifying the name of the anti-spam product to use to scan a message for spam (defaults to SpamAssassin)
     *
     * @return the spam scanning engine to use.
     * @hibernate.property
     * column="SPAM_SCANNER"
     * type="com.metavize.tran.email.SScannerUserType"
     * not-null="true"
     */
    public SScanner getSpamScanner()
    {
        return spamScanner;
    }

    public void setSpamScanner(SScanner spamScanner)
    {
        // Guard XXX
        this.spamScanner = spamScanner;
    }

    public String[] getSpamScannerEnumeration()
    {
        SScanner[] azSScanner = SScanner.values();
        String[] result = new String[azSScanner.length];
        for (int i = 0; i < azSScanner.length; i++)
            result[i] = azSScanner[i].toString();
        return result;
    }

    /**
     * - virusScanner: a string specifying the name of the anti-virus product to use to scan a message for virus (defaults to F-Prot)
     *
     * @return the virus scanning engine to use.
     * @hibernate.property
     * column="VIRUS_SCANNER"
     * type="com.metavize.tran.email.VScannerUserType"
     * not-null="true"
     */
    public VScanner getVirusScanner()
    {
        return virusScanner;
    }

    public void setVirusScanner(VScanner virusScanner)
    {
        // Guard XXX
        this.virusScanner = virusScanner;
    }

    public String[] getVirusScannerEnumeration()
    {
        VScanner[] azVScanner = VScanner.values();
        String[] result = new String[azVScanner.length];
        for (int i = 0; i < azVScanner.length; i++)
            result[i] = azVScanner[i].toString();
        return result;
    }

    /**
     * - pop3Postmaster: a string identifying e-mail address of POP3 server postmaster (defaults to "POP3 Postmaster <postmaster@localhost>")
     *
     * @return the pop3 postmaster email address
     * @hibernate.property
     * column="POP3_POSTMASTER"
     */
    public String getPop3Postmaster()
    {
        return pop3Postmaster;
    }

    public void setPop3Postmaster(String pop3Postmaster)
    {
        // Guard XXX
        this.pop3Postmaster = pop3Postmaster;
    }

    /**
     * - imap4Postmaster: a string identifying e-mail address of IMAP4 server postmaster (defaults to "IMAP4 Postmaster <postmaster@localhost>")
     *
     * @return the imap4 postmaster email address
     * @hibernate.property
     * column="IMAP4_POSTMASTER"
     */
    public String getImap4Postmaster()
    {
        return imap4Postmaster;
    }

    public void setImap4Postmaster(String imap4Postmaster)
    {
        // Guard XXX
        this.imap4Postmaster = imap4Postmaster;
    }

    /**
     * - copyOnException: a boolean specifying whether or not to save a copy of message when an exception occurs (defaults to false)
     *
     * @return whether or not to save a copy of message when exception occurs
     * @hibernate.property
     * column="COPY_ON_EXCEPTION"
     */
    public boolean isCopyOnException()
    {
        return copyOnException;
    }

    public void setCopyOnException(boolean copyOnException)
    {
        this.copyOnException = copyOnException;
    }

    /**
     * - msgSzLimit: an int specifying the maximum size (in bytes) of a message to send/receive (message must be 2KB or greater but less than 10MB - we enforce minimum message size limit because we may need to access message header later and assume that header will always fit in 1st 2KB) (defaults to 8MB)
     *
     * @return how big a message to allow
     * @hibernate.property
     * column="MSG_SZ_LIMIT"
     */
    public int getMsgSzLimit()
    {
        return msgSzLimit;
    }

    public void setMsgSzLimit(int msgSzLimit)
    {
        // Guard XXX
        this.msgSzLimit = msgSzLimit;
    }

    /**
     * - spamMsgSzLimit: an int specifying the maximum size (in bytes) of a message to scan for spam (if size is -1, then all messages (up to msgSzLimit in size) will be scanned) (defaults to 256Kb == 262,144b)
     *
     * @return maximum size of messages to scan for spam
     * @hibernate.property
     * column="SPAM_MSG_SZ_LIMIT"
     */
    public int getSpamMsgSzLimit()
    {
        return spamMsgSzLimit;
    }

    public void setSpamMsgSzLimit(int spamMsgSzLimit)
    {
        // Guard XXX
        this.spamMsgSzLimit = spamMsgSzLimit;
    }

    /**
     * - virusMsgSzLimit: an int specifying the maximum size (in bytes) of a message to scan for viruses (if size is -1, then all messages (up to msgSzLimit in size) will be scanned) (defaults to -1)
     *
     * @return maximum size of messages to scan for virus
     * @hibernate.property
     * column="VIRUS_MSG_SZ_LIMIT"
     */
    public int getVirusMsgSzLimit()
    {
        return virusMsgSzLimit;
    }

    public void setVirusMsgSzLimit(int virusMsgSzLimit)
    {
        // Guard XXX
        this.virusMsgSzLimit = virusMsgSzLimit;
    }

    /**
     * - returnErrOnSMTPBlock: a boolean specifying whether or not to return an error code when a message from a SMTP server is blocked (otherwise, message is silently blocked) (defaults to false)
     *
     * @return whether or not to return an error code when blocked
     * @hibernate.property
     * column="RETURN_ERR_ON_SMTP_BLOCK"
     */
    public boolean isReturnErrOnSMTPBlock()
    {
        return returnErrOnSMTPBlock;
    }

    public void setReturnErrOnSMTPBlock(boolean returnErrOnSMTPBlock)
    {
        this.returnErrOnSMTPBlock = returnErrOnSMTPBlock;
    }

    /**
     * - returnErrOnPOP3Block: a boolean specifying whether or not to return an error code when a message from a POP3 server is blocked (otherwise, a "message has been blocked" warning message is returned instead of an error code) (defaults to false)
     *
     * @return whether or not to return an error code when blocked
     * @hibernate.property
     * column="RETURN_ERR_ON_POP3_BLOCK"
     */
    public boolean isReturnErrOnPOP3Block()
    {
        return returnErrOnPOP3Block;
    }

    public void setReturnErrOnPOP3Block(boolean returnErrOnPOP3Block)
    {
        this.returnErrOnPOP3Block = returnErrOnPOP3Block;
    }

    /**
     * - returnErrOnIMAP4Block: a boolean specifying whether or not to return an error code when a message from a IMAP4 server is blocked (otherwise, a "message has been blocked" warning message is returned instead of an error code) (defaults to false)
     *
     * @return whether or not to return an error code when blocked
     * @hibernate.property
     * column="RETURN_ERR_ON_IMAP4_BLOCK"
     */
    public boolean isReturnErrOnIMAP4Block()
    {
        return returnErrOnIMAP4Block;
    }

    public void setReturnErrOnIMAP4Block(boolean returnErrOnIMAP4Block)
    {
        this.returnErrOnIMAP4Block = returnErrOnIMAP4Block;
    }

    /**
     * - imap4PostmasterDetails: a string giving details of imap4Postmaster field, defaults to NO_DETAILS
     *
     * @return the imap4Postmaster details
     * @hibernate.property
     * column="DETAILS_IMAP4_POSTMASTER"
     */
    public String getImap4PostmasterDetails()
    {
        return imap4PostmasterDetails;
    }

    public void setImap4PostmasterDetails(String imap4PostmasterDetails)
    {
        this.imap4PostmasterDetails = imap4PostmasterDetails;
    }

    /**
     * - pop3PostmasterDetails: a string giving details of pop3Postmaster field, defaults to NO_DETAILS
     *
     * @return the pop3Postmaster details
     * @hibernate.property
     * column="DETAILS_POP3_POSTMASTER"
     */
    public String getPop3PostmasterDetails()
    {
        return pop3PostmasterDetails;
    }

    public void setPop3PostmasterDetails(String pop3PostmasterDetails)
    {
        this.pop3PostmasterDetails = pop3PostmasterDetails;
    }

    /**
     * - msgSzLimitDetails: a string giving details of msgSzLimit field, defaults to NO_DETAILS
     *
     * @return the msgSzLimit details
     * @hibernate.property
     * column="DETAILS_MSG_SZ_LIMIT"
     */
    public String getMsgSzLimitDetails()
    {
        return msgSzLimitDetails;
    }

    public void setMsgSzLimitDetails(String msgSzLimitDetails)
    {
        this.msgSzLimitDetails = msgSzLimitDetails;
    }

    /**
     * - spamSzLimitDetails: a string giving details of spamSzLimit field, defaults to NO_DETAILS
     *
     * @return the spamSzLimit details
     * @hibernate.property
     * column="DETAILS_SPAM_SZ_LIMIT"
     */
    public String getSpamSzLimitDetails()
    {
        return spamSzLimitDetails;
    }

    public void setSpamSzLimitDetails(String spamSzLimitDetails)
    {
        this.spamSzLimitDetails = spamSzLimitDetails;
    }

    /**
     * - virusSzLimitDetails: a string giving details of virusSzLimit field, defaults to NO_DETAILS
     *
     * @return the virusSzLimit details
     * @hibernate.property
     * column="DETAILS_VIRUS_SZ_LIMIT"
     */
    public String getVirusSzLimitDetails()
    {
        return virusSzLimitDetails;
    }

    public void setVirusSzLimitDetails(String virusSzLimitDetails)
    {
        this.virusSzLimitDetails = virusSzLimitDetails;
    }

    /**
     * - alertsDetails: a string giving details of alerts field, defaults to NO_DETAILS
     *
     * @return the alerts details
     * @hibernate.property
     * column="DETAILS_ALERTS"
     */
    public String getAlertsDetails()
    {
        return alertsDetails;
    }

    public void setAlertsDetails(String alertsDetails)
    {
        this.alertsDetails = alertsDetails;
    }

    /**
     * - logDetails: a string giving details of log field, defaults to NO_DETAILS
     *
     * @return the log details
     * @hibernate.property
     * column="DETAILS_LOG"
     */
    public String getLogDetails()
    {
        return logDetails;
    }

    public void setLogDetails(String logDetails)
    {
        this.logDetails = logDetails;
    }
}
