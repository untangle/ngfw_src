/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProductImpl.java 11648 2007-07-05 18:55:29Z rbscott $
 */

package com.untangle.uvm.rup;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Proxy nonce.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_proxy_nonce", schema="settings")
public class ProxyNonce
{
    private Long id;
    private String nonce;
    private Date createTime;

    // constructors -----------------------------------------------------------

    public ProxyNonce() { }

    public ProxyNonce(String nonce)
    {
        this.nonce = nonce;
        this.createTime = new Date();
    }

    // accessors --------------------------------------------------------------

    @Id
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    public String getNonce()
    {
        return nonce;
    }

    public void setNonce(String nonce)
    {
        this.nonce = nonce;
    }

    @Column(name="create_time")
    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}