/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusRuleEvent.java,v 1.9 2005/03/25 03:51:16 amread Exp $
 */
package com.metavize.tran.email;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.virus.VirusScannerResult;

/**
 * Log e-mail message virus rule (message contains virus) event.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_VIRUS_EVT"
 * mutable="false"
 */
public class VirusRuleEvent extends LogEvent
{
    /* constants */
    public final static String EMPTY_NAME = "";

    /* class variables */

    /* instance variables */
    private MLMessageInfo zMsgInfo; /* msg_id */
    private char zAction;
    private String zName;
    private boolean bIsClean;
    private boolean bRemoved;

    /* constructors */
    public VirusRuleEvent() {}

    public VirusRuleEvent(MLMessageInfo zMsgInfo, Action zAction, VirusScannerResult zScanResult)
    {
        this.zMsgInfo = zMsgInfo;
        this.zAction = zAction.getKey();
        if (null == zScanResult)
        {
            zName = EMPTY_NAME;
            bIsClean = true;
            bRemoved = false;
        }
        else
        {
            zName = zScanResult.getVirusName();
            bIsClean = zScanResult.isClean();
            bRemoved = zScanResult.isVirusCleaned();
        }
    }

    /* public methods */
    /**
     * Associate e-mail message info with event.
     *
     * @return e-mail message info.
     * @hibernate.many-to-one
     * column="MSG_ID"
     * cascade="all"
     */
    public MLMessageInfo getMLMessageInfo()
    {
        return zMsgInfo;
    }

    public void setMLMessageInfo(MLMessageInfo zMsgInfo)
    {
        this.zMsgInfo = zMsgInfo;
        return;
    }

    /**
     * Identify result of virus rule action.
     * type="com.metavize.tran.email.ActionUserType"
     *
     * @return result of virus rule action.
     * @hibernate.property
     * column="ACTION"
     * not-null="true"
     */
    public char getAction()
    {
        return zAction;
    }

    public void setAction(char zAction)
    {
        this.zAction = zAction;
        return;
    }

    /**
     * Identify name of virus.
     *
     * @return virus name.
     * @hibernate.property
     * column="NAME"
     * not-null="true"
     */
    public String getName()
    {
        return zName;
    }

    public void setName(String zName)
    {
        this.zName = zName;
        return;
    }

    /**
     * Identify (original) e-mail message infection status.
     *
     * @return (original) e-mail message infection status.
     * @hibernate.property
     * column="IS_CLEAN"
     * not-null="true"
     */
    public boolean getIsClean()
    {
        return bIsClean;
    }

    public void setIsClean(boolean bIsClean)
    {
        this.bIsClean = bIsClean;
        return;
    }

    /**
     * Identify e-mail message dis-infection status.
     *
     * @return if e-mail message dis-infection status.
     * @hibernate.property
     * column="DISINFECTED"
     * not-null="true"
     */
    public boolean getRemoved()
    {
        return bRemoved;
    }

    public void setRemoved(boolean bRemoved)
    {
        this.bRemoved = bRemoved;
        return;
    }

    /* private methods */
}
