/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: BlockTemplate.java,v 1.5 2005/03/15 02:11:53 amread Exp $
 */

package com.metavize.tran.httpblocker;

import java.io.Serializable;
import java.net.URI;

/**
 * Message to be displayed when a message is blocked.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTPBLK_TEMPLATE"
 * mutable="false"
 */
public class BlockTemplate implements Serializable
{
    private static final long serialVersionUID = -2176543704833470091L;

    // XXX someone, make this pretty
    private static final String BLOCK_TEMPLATE
        = "<html><center><b>%s</b></center>"
        + "<p>This site blocked because of inappropriate content</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Category: %s</p>"
        + "<p>Please contact %s</p>";

    private Long id;
    private String header = "Metavize Content Filter";
    private String contact = "your network administrator";

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public BlockTemplate() { }

    public BlockTemplate(String header, String contact)
    {
        this.header = header;
        this.contact = contact;
    }

    // business methods ------------------------------------------------------

    public String render(String host, URI uri, String category)
    {
        return String.format(BLOCK_TEMPLATE, header, host, uri, category,
                             contact);
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="MESSAGE_ID"
     * generator-class="native"
     */
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Customizable banner on the block page.
     *
     * @return the header.
     * @hibernate.property
     * column="HEADER"
     */
    public String getHeader()
    {
        return header;
    }

    public void setHeader(String header)
    {
        this.header = header;
    }

    /**
     * Contact information.
     *
     * @return a <code>String</code> value
     * @hibernate.property
     * column="CONTACT"
     */
    public String getContact()
    {
        return contact;
    }

    public void setContact(String contact)
    {
        this.contact = contact;
    }
}
