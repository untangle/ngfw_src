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
package com.metavize.tran.mail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.tapi.IPSession;
import org.apache.log4j.Logger;

/**
 * Log e-mail message info.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_MAIL_MESSAGE_INFO"
 * mutable="false"
 */
public class MessageInfo implements Serializable
{
    /* constants */
    public static final int SMTP_PORT = 25;
    public static final int POP3_PORT = 110;
    public static final int IMAP4_PORT = 143;

    // private static final Logger zLog = Logger.getLogger(MessageInfo.class.getName());

    /* columns */
    private Long id; /* msg_id */

    private int sessionId; /* s_id */
    // private MLHandlerInfo handlerInfo; /* hdl_id */

    private String subject;

    private char serverType;

    /* Senders/Receivers */
    private List addressList = new ArrayList();

    /* constructors */
    public MessageInfo() {}

    public MessageInfo(IPSession session, String subject)
    {
        this.subject = subject;
        this.sessionId = session.id();

        // Hack for figuring out protocol.  Works fine until we move to dynamic pipelines. XXX
        int serverPort = session.serverPort();
        switch (serverPort) {
        case SMTP_PORT:
            serverType = 'S';
            break;
        case POP3_PORT:
            serverType = 'P';
            break;
        case IMAP4_PORT:
            serverType = 'I';
            break;
        default:
            serverType = 'U';
            break;
        }
    }

    /* Business methods */
    public void addAddress(AddressKind kind, String address, String personal)
    {
        MessageInfoAddr newAddr = new MessageInfoAddr(kind, address, personal);
        addressList.add(newAddr);
    }

    /* public methods */

    /**
     *
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * List of the addresses involved (to, from, etc) in the email.
     *
     * @return the list of the email addresses involved in the email
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="MSG_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.mail.MessageInfoAddr"
     */
    public List getAddresses()
    {
        return addressList;
    }

    public void setAddresses( List s )
    {
        addressList = s;
    }

    /**
     * Session id.
     *
     * @return the session id.
     * @hibernate.property
     * column="SESSION_ID"
     */
    public int getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(int sessionId)
    {
        this.sessionId = sessionId;
    }

    /**
     * Identify RFC822 Subject.
     *
     * @return RFC822 Subject.
     * @hibernate.property
     * column="SUBJECT"
     */
    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    /**
     * Identify server type (SMTP, POP3, or IMAP4).
     *
     * @return server type.
     * @hibernate.property
     * column="SERVER_TYPE"
     * type="char"
     * length="1"
     * not-null="true"
     */
    public char getServerType()
    {
        return serverType;
    }

    public void setServerType(char serverType)
    {
        this.serverType = serverType;
    }
}
