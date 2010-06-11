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

package com.untangle.node.mail.papi.safelist;

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
 * Setting for safelist (recipient and sender pair).
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_mail_safels_settings", schema="settings")
@SuppressWarnings("serial")
public class SafelistSettings implements Serializable
{

    private Long id;

    private SafelistRecipient recipient;
    private SafelistSender sender;

    // constructors -----------------------------------------------------------

    public SafelistSettings() {}

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="safels_id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;

        return;
    }

    // - a recipient may define a set of safelists (with approved senders)
    // - each safelist contains one recipient and one sender
    // - each recipient or sender may be used several times in safelists
    // (e.g., a recipient may use many (different) senders on his safelists or
    //  many (different) recipients may use the same sender on their safelists)
    // - to keep safelist tables manageable,
    // we only create a single reference of each recipient or sender and
    // share each recipient or sender reference between multiple safelists
    //
    // - with hibernate many-to-one mapping and
    // all cascade (save-update-delete cascade),
    // when we delete a safelist
    // (e.g., drop a safelist by not including its hibernate mapping object
    //  in the collection that we saveOrUpdate),
    // hibernate believes that it must cascade this change
    // by deleting the safelist and
    // deleting the recipient and sender that the safelist references too
    // but realizes that the recipient or sender is still in use and
    // cannot be deleted
    // (other safelists continue to reference the recipient or sender):
    //
    // 11 11:10:17.405 ERROR [TransactionRunner] something bad happened, not retrying
    // org.hibernate.ObjectDeletedException: deleted object would be re-saved by cascade (remove deleted object from associations): [com.untangle.node.mail.papi.safelist.SafelistRecipient#26762]
    // at org.hibernate.impl.SessionImpl.forceFlush(SessionImpl.java:742)
    // at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.entityIsTransient(DefaultSaveOrUpdateEventListener.java:166)
    // at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.performSaveOrUpdate(DefaultSaveOrUpdateEventListener.java:96)
    // at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.onSaveOrUpdate(DefaultSaveOrUpdateEventListener.java:69)
    // at org.hibernate.impl.SessionImpl.merge(SessionImpl.java:468)
    //
    // --> this error text implies that
    //     all cascade actually means delete-save-update cascade and
    //     does not mean save-update-delete cascade
    //
    // - with hibernate many-to-one mapping and save-update cascade,
    // when we delete a safelist
    // hibernate doesn't cascade this change and
    // only deletes the safelist
    // (it orphans the recipient and sender that the safelist references)
    //
    // - with hibernate many-to-any mapping and
    // all cascade (save-update-delete cascade),
    // when we delete a safelist
    // hibernate may correctly cascade this change
    // (it may delete the safelist and
    //  check that no other safelist references the recipient and sender
    //  before it attempts to delete the recipient or sender)
    // but because xdoclet doesn't support many-to-any mapping,
    // we can't confirm if this is what hibernate will do
    //
    // *** for now, we use many-to-one mapping and save-update cascade and
    // *** let hibernate save and update parent/child changes and
    // *** we will clean up any orphaned children
    // *** during nightly (report) roll ups

    // * cascade="all" //original
    /**
     * @return the recipient of this safelist
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="recipient")
    public SafelistRecipient getRecipient()
    {
        return recipient;
    }

    public void setRecipient(SafelistRecipient recipient)
    {
        this.recipient = recipient;

        return;
    }

    // * cascade="all" //original
    /**
     * @return the sender of this safelist
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="sender", nullable=false)
    public SafelistSender getSender()
    {
        return sender;
    }

    public void setSender(SafelistSender sender)
    {
        this.sender = sender;

        return;
    }

    // Object methods
    public boolean equals(Object o)
    {
        if (!(o instanceof SafelistSettings)) {
            return false;
        }

        SafelistSettings sls = (SafelistSettings)o;
        return (true == recipient.equals(sls.recipient) &&
                true == sender.equals(sls.sender));
    }
}
