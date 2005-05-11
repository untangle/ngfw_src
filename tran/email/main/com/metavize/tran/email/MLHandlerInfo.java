/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.email;

import com.metavize.tran.util.CBufferWrapper;

/**
 * Log e-mail handler info.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_HANDLER_INFO"
 * mutable="false"
 */
public class MLHandlerInfo
{
    /* constants */

    /* instance variables */
    private Long zId; /* hdl_id */
    private int zSessionId; /* s_id */
    /* for SMTP,
     * RFC 821 states that
     * each service greeting must include official name of server host
     * (e.g., they don't include name or provide IP address instead of name):
     *
     *    Note: all the greeting type replies have the official name of
     *    the server host as the first word following the reply code.
     *       For example,
     *          220 <SP> USC-ISIF.ARPA <SP> Service ready <CRLF>
     *
     * but some SMTP implementations do not follow this part of standard
     * -> therefore, we will not parse service greeting to retrieve name
     * -> since POP3 and IMAP4 do not require name of server host or
     *    specify any format, we cannot easily retrieve name anyway
    //private String zSvrHost;
     */
    private String zUserName;
    private String zSrvGreeting;
    private char cSvrType;

    /* constructors */
    public MLHandlerInfo() {}

    public MLHandlerInfo(int zSessionId, char cSvrType, CBufferWrapper zSrvGreeting)
    {
        this.zSessionId = zSessionId;
        this.cSvrType = cSvrType;
        //this.zSvrHost = zSvrHost;
        /* we copy original backing data by stringifying it */
        zUserName = null; /* we can't set until handler retrieves it */
        this.zSrvGreeting = zSrvGreeting.toString();
    }

    /* public methods */
    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return zId;
    }

    private void setId(Long zId)
    {
        this.zId = zId;
        return;
    }

    /**
     * Associate e-mail handler info with pipeline info.
     *
     * @return pipeline info.
     * @hibernate.property
     * column="S_ID"
     */
    public int getSessionId()
    {
        return zSessionId;
    }

    public void setSessionId(int zSessionId)
    {
        this.zSessionId = zSessionId;
        return;
    }

    ///**
     //* Identify server host (name of SMTP server; insert null for POP and IMAP servers since RFCs don't require that servers identify themselves).
     //*
     //* @return server host.
     //* @hibernate.property
     //* column="SVR_HOST"
     //*/
    //public String getServerHost()
    //{
        //return zSvrHost;
    //}

    //public void setServerHost(String zSvrHost)
    //{
        //this.zSvrHost = zSvrHost;
        //return;
    //}

    /**
     * Identify server type (SMTP, POP3, or IMAP4).
     *
     * @return server type.
     * @hibernate.property
     * column="SVR_TYPE"
     * type="char"
     * length="1"
     * not-null="true"
     */
    public char getServerType()
    {
        return cSvrType;
    }

    public void setServerType(char cSvrType)
    {
        this.cSvrType = cSvrType;
        return;
    }

    /**
     * Identify user name (only relevant for IMAP and POP if not encoded/encrypted).
     *
     * @return user name.
     * @hibernate.property
     * column="USER_NAME"
     */
    public String getUserName()
    {
        return zUserName;
    }

    public void setUserName(String zUserName)
    {
        this.zUserName = zUserName;
        return;
    }

    public void setUserName(CBufferWrapper zUserName)
    {
        this.zUserName = zUserName.toString();
        return;
    }

    /**
     * Identify service connection greeting.
     *
     * @return service connection greeting.
     * @hibernate.property
     * column="SRV_GREETING"
     * length="1024"
     * not-null="true"
     */
    public String getSrvGreeting()
    {
        return zSrvGreeting;
    }

    public void setSrvGreeting(String zSrvGreeting)
    {
        this.zSrvGreeting = zSrvGreeting;
        return;
    }

    /* private methods */
}
