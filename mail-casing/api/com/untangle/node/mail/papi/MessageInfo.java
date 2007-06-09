/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.mail.papi;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Log e-mail message info.
 *
 * @author <a href="mailto:cng@untangle.com">C Ng</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_mail_message_info", schema="events")
    public class MessageInfo implements Serializable
    {
        /* constants */
        public static final int SMTP_PORT = 25;
        public static final int POP3_PORT = 110;
        public static final int IMAP4_PORT = 143;

        // How big a varchar() do we get for default String fields.  This
        // should be elsewhere. XXX
        public static final int DEFAULT_STRING_SIZE = 255;

        /* columns */
        private Long id; /* msg_id */
        private PipelineEndpoints pipelineEndpoints;
        private String subject;
        private char serverType;
        private Date timeStamp = new Date();

        /* Senders/Receivers */
        private Set<MessageInfoAddr> addresses = new HashSet<MessageInfoAddr>();

        /* non-persistent fields */
        public Map counts = new HashMap();

        /* constructors */
        public MessageInfo() { }

        public MessageInfo(PipelineEndpoints pe, int serverPort, String subject)
        {
            pipelineEndpoints = pe;

            // Subject really shouldn't be NOT NULL, but it's easier for
            // now to fix by using an empty string... XXX jdi 8/9/05
            if (subject == null)
                subject = "";

            if (subject != null && subject.length() > DEFAULT_STRING_SIZE) {
                subject = subject.substring(0, DEFAULT_STRING_SIZE);
            }
            this.subject = subject;

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
            Integer p = (Integer)counts.get(kind);
            if (null == p) {
                p = 0;
            }
            counts.put(kind, ++p);

            MessageInfoAddr newAddr = new MessageInfoAddr(this, p, kind, address, personal);
            addresses.add(newAddr);
            return;
        }

        /* public methods */

        @Id
        @Column(name="id")
        @GeneratedValue
        private Long getId()
        {
            return id;
        }

        private void setId(Long id)
        {
            this.id = id;
            return;
        }

        /**
         * Set of the addresses involved (to, from, etc) in the email.
         *
         * @return the set of the email addresses involved in the email
         */
        @OneToMany(mappedBy="messageInfo", cascade=CascadeType.ALL,
                   fetch=FetchType.EAGER)
        public Set<MessageInfoAddr> getAddresses()
        {
            return addresses;
        }

        public void setAddresses(Set<MessageInfoAddr> s)
        {
            addresses = s;
            return;
        }

        /**
         * Get the PipelineEndpoints.
         *
         * @return the PipelineEndpoints.
         */
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="pl_endp_id", nullable=false)
        public PipelineEndpoints getPipelineEndpoints()
        {
            return pipelineEndpoints;
        }

        public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
        {
            this.pipelineEndpoints = pipelineEndpoints;
            return;
        }

        /**
         * Identify RFC822 Subject.
         *
         * @return RFC822 Subject.
         */
        @Column(nullable=false)
        public String getSubject()
        {
            return subject;
        }

        public void setSubject(String subject)
        {
            if (subject != null && subject.length() > DEFAULT_STRING_SIZE) {
                subject = subject.substring(0, DEFAULT_STRING_SIZE);
            }
            this.subject = subject;
            return;
        }

        /**
         * Identify server type (SMTP, POP3, or IMAP4).
         *
         * @return server type.
         */
        @Column(name="server_type", length=1, nullable=false)
        public char getServerType()
        {
            return serverType;
        }

        public void setServerType(char serverType)
        {
            this.serverType = serverType;
            return;
        }

        /**
         * Identify approximate datetime that this message was received.
         *
         * @return datetime of message.
         */
        @Temporal(TemporalType.TIMESTAMP)
        @Column(name="time_stamp")
        public Date getTimeStamp()
        {
            return timeStamp;
        }

        public void setTimeStamp(Date timeStamp)
        {
            this.timeStamp = timeStamp;
            return;
        }
    }
