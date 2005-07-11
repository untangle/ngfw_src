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

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * Log e-mail message info.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="EMAIL_MESSAGE_INFO"
 * mutable="false"
 */
public class MessageInfo implements Serializable
{
    /* constants */
    // private static final Logger zLog = Logger.getLogger(MessageInfo.class.getName());

    /* columns */
    private Long id; /* msg_id */
    // private MLHandlerInfo handlerInfo; /* hdl_id */

    private String subject;

    /* Senders/Receivers */
    private List addressList = new ArrayList();

    /* constructors */
    public MessageInfo() {}

    public MessageInfo(String subject)
    {
        this.subject = subject;
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
}
