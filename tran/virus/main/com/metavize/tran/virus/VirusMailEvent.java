/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusLogEvent.java 502 2005-04-28 03:31:42Z amread $
 */

package com.metavize.tran.virus;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.mail.papi.MessageInfo;

/**
 * Log for POP3/IMAP Virus events.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_EVT_MAIL"
 * mutable="false"
 */
public class VirusMailEvent extends LogEvent
{
    private MessageInfo messageInfo;
    private VirusScannerResult result;
    private VirusMessageAction action;
    private String vendorName;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusMailEvent() { }

    public VirusMailEvent(MessageInfo messageInfo, VirusScannerResult result, VirusMessageAction action,
                          String vendorName)
    {
        this.messageInfo = messageInfo;
        this.result = result;
        this.action = action;
        this.vendorName = vendorName;
    }

    // accessors --------------------------------------------------------------

    /**
     * Associate e-mail message info with event.
     *
     * @return e-mail message info.
     * @hibernate.many-to-one
     * column="MSG_ID"
     * cascade="save-update"
     */
    public MessageInfo getMessageInfo()
    {
        return messageInfo;
    }

    public void setMessageInfo(MessageInfo messageInfo)
    {
        this.messageInfo = messageInfo;
    }

    /**
     * Virus scan result.
     *
     * @return the scan result.
     * @hibernate.property
     * cascade="save-update"
     * type="com.metavize.tran.virus.VirusScannerResultUserType"
     * @hibernate.column
     * name="CLEAN"
     * @hibernate.column
     * name="VIRUS_NAME"
     * @hibernate.column
     * name="VIRUS_CLEANED"
     */
    public VirusScannerResult getResult()
    {
        return result;
    }

    public void setResult(VirusScannerResult result)
    {
        this.result = result;
    }

    /**
     * The action taken
     *
     * @return action.
     * @hibernate.property
     * type="com.metavize.tran.virus.VirusMessageActionUserType"
     * column="ACTION"
     */
    public VirusMessageAction getAction()
    {
        return action;
    }

    public void setAction(VirusMessageAction action)
    {
        this.action = action;
    }

    /**
     * Spam scanner vendor.
     *
     * @return the vendor
     * @hibernate.property
     * column="VENDOR_NAME"
     */
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }
}
