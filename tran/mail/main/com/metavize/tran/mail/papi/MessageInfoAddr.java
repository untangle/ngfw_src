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
package com.metavize.tran.mail.papi;

import java.io.Serializable;

/**
 * Log e-mail message info.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_MAIL_MESSAGE_INFO_ADDR"
 * mutable="false"
 */
public class MessageInfoAddr implements Serializable
{
    /* constants */
    //    private static final Logger logger = Logger.getLogger(MessageInfo.class.getName());

    /* columns */
    private Long id; /* msg_id */
    // private MLHandlerInfo handlerInfo; /* hdl_id */

    private MessageInfo messageInfo;
    private int position;
    private AddressKind kind;
    private String addr;
    private String personal;

    /* constructors */
    public MessageInfoAddr() {}

    public MessageInfoAddr(MessageInfo messageInfo, int position,
                           AddressKind kind, String addr, String personal) {
        this.messageInfo = messageInfo;
        this.position = position;
        this.kind = kind;
        if (addr.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
        }
        this.addr = addr;
        if (personal != null
            && personal.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            personal = personal.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
        }
        this.personal = personal;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    /**
     * The MessageInfo object.
     * @hibernate.many-to-one
     * column="MSG_ID"
     * not-null="true"
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
     * The relative position of the field in the set. XXX yes, its a
     * dirty hack, but this enables us to do INSERT without an UPDATE
     * and also helps the reporting.
     *
     * @return the relative position to other MessageInfoAddr
     * @hibernate.property
     * column="POSITION"
     * not-null="true"
     */
    public int getPosition()
    {
        return position;
    }

    public void setPosition(int position)
    {
        this.position = position;
    }

    /**
     * The email address, in RFC822 format
     *
     * @return email address.
     * @hibernate.property
     * column="ADDR"
     * not-null="true"
     */
    public String getAddr()
    {
        return addr;
    }

    public void setAddr(String addr)
    {
        if (addr.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
        }
        this.addr = addr;
    }

    /**
     * Get a personal for display purposes.
     *
     * @return personal.
     * @hibernate.property
     * column="PERSONAL"
     */
    public String getPersonal()
    {
        return personal;
    }

    public void setPersonal(String personal)
    {
        if (personal != null
            && personal.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            personal = personal.substring(0,MessageInfo.DEFAULT_STRING_SIZE);
        }
        this.personal = personal;
    }

    /**
     * The kind of address (To, CC, etc).
     *
     * @return addressKind.
     * @hibernate.property
     * type="com.metavize.tran.mail.papi.AddressKindUserType"
     * column="KIND"
     */
    public AddressKind getKind()
    {
        return kind;
    }

    public void setKind(AddressKind king)
    {
        this.kind = kind;
    }
}
