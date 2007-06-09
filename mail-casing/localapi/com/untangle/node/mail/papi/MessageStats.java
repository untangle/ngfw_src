/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary statsrmation of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Statsrmation.
 *
 * $Id$
 */

package com.untangle.node.mail.papi;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Log e-mail message stats.
 *
 * @author <a href="mailto:cng@untangle.com">C Ng</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_mail_message_stats", schema="events")
    public class MessageStats implements Serializable
    {
        /* constants */

        /* columns */
        private Long id; /* id */

        private MessageInfo messageInfo; /* msg_id */

        private int numAttachments;

        private long numBytes;

        /* constructors */
        public MessageStats() { }

        public MessageStats(MessageInfo messageInfo, int numAttachments, long numBytes)
        {
            this.messageInfo = messageInfo;
            this.numAttachments = numAttachments;
            this.numBytes = numBytes;
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
        }

        /**
         * Associate e-mail message stats with e-mail message info.
         *
         * @return e-mail message info.
         */
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="msg_id")
        public MessageInfo getMessageInfo()
        {
            return messageInfo;
        }

        public void setMessageInfo(MessageInfo messageInfo)
        {
            this.messageInfo = messageInfo;
        }


        /**
         * Total bytes in message, (body + header?  just body? XX)
         *
         * @return the number of bytes in the message
         */
        @Column(name="msg_bytes", nullable=false)
        public long getNumBytes()
        {
            return numBytes;
        }

        public void setNumBytes(long numBytes)
        {
            this.numBytes = numBytes;
        }

        /**
         * Total attachments in message, (body + header?  just body? XX)
         *
         * @return the number of attachments in the message
         */
        @Column(name="msg_attachments", nullable=false)
        public int getNumAttachments()
        {
            return numAttachments;
        }

        public void setNumAttachments(int numAttachments)
        {
            this.numAttachments = numAttachments;
        }
    }
