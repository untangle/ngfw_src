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
import com.metavize.tran.mail.papi.smtp.SMTPNotifyAction;
import com.metavize.tran.mail.papi.smtp.SMTPNotifyActionUserType;

/**
 * Log for SMTP Virus events.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_EVT_SMTP"
 * mutable="false"
 */
public class VirusSmtpEvent extends LogEvent
{
    private MessageInfo messageInfo;
    private VirusScannerResult result;
    private SMTPVirusMessageAction action;
    private SMTPNotifyAction notifyAction;
    private String vendorName;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusSmtpEvent() { }

    public VirusSmtpEvent(MessageInfo messageInfo, VirusScannerResult result,
                          SMTPVirusMessageAction action,
                          SMTPNotifyAction notifyAction, String vendorName)
    {
        this.messageInfo = messageInfo;
        this.result = result;
        this.action = action;
        this.notifyAction = notifyAction;
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
     * type="com.metavize.tran.virus.SMTPVirusMessageActionUserType"
     * column="ACTION"
     */
    public SMTPVirusMessageAction getAction()
    {
        return action;
    }

    public void setAction(SMTPVirusMessageAction action)
    {
        this.action = action;
    }

    /**
     * The notify action taken
     *
     * @return action.
     * @hibernate.property
     * type="com.metavize.tran.mail.papi.smtp.SMTPNotifyActionUserType"
     * column="NOTIFY_ACTION"
     */
    public SMTPNotifyAction getNotifyAction()
    {
        return notifyAction;
    }

    public void setNotifyAction(SMTPNotifyAction notifyAction)
    {
        this.notifyAction = notifyAction;
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
