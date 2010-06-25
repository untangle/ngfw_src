/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
@Table(name="n_mail_message_info", schema="events")
@SuppressWarnings("serial")
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
    public Map<AddressKind,Integer> counts = new HashMap<AddressKind,Integer>();

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

    @SuppressWarnings("unused")
	@Id
    @Column(name="id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    @SuppressWarnings("unused")
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
    @OneToMany(mappedBy="messageInfo", cascade=CascadeType.ALL, fetch=FetchType.EAGER)
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
