/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Iterator;
import java.util.List;
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
import javax.persistence.Transient;

import com.metavize.mvvm.security.Tid;

/**
 * Message to be displayed when a message is blocked.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_httpblk_template", schema="settings")
public class BlockTemplate implements Serializable
{
    private static final long serialVersionUID = -2176543704833470091L;

    private static final String TEMPLATE_NAME
        = "com/metavize/tran/httpblocker/blocktemplate.html";

    private static String BLOCK_TEMPLATE;

    private Long id;
    private String header = "Untangle Networks Content Filter";
    private String contact = "your network administrator";

    // constructor ------------------------------------------------------------

    public BlockTemplate() { }

    public BlockTemplate(String header, String contact)
    {
        this.header = header;
        this.contact = contact;
    }

    // business methods ------------------------------------------------------

    public String render(String servletBase, String host, URI uri,
                         String category)
    {
        if (null == BLOCK_TEMPLATE) {
            synchronized (BlockTemplate.class) {
                if (null == BLOCK_TEMPLATE) {
                    BLOCK_TEMPLATE = "";

                    InputStream is = BlockTemplate.class.getClassLoader()
                        .getResourceAsStream(TEMPLATE_NAME);
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String l;
                    try {
                        while (null != (l = br.readLine())) {
                            BLOCK_TEMPLATE += l + "\n";
                        }
                    } catch (IOException exn) {
                        BLOCK_TEMPLATE = null; // XXX try again next time
                    }
                }
            }
        }

        if (null == BLOCK_TEMPLATE) {
            return "Blocked Site: http://" + host + uri;
        } else {
            return String.format(BLOCK_TEMPLATE, servletBase, header, host, uri,
                                 category, contact);
        }
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="message_id")
    @GeneratedValue
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
