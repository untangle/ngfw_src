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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.Rule;


/**
 * Class used to associate two email addresses
 */
@Entity
@Table(name="email_addr_pair_rule", schema="settings")
public class EmailAddressPairRule extends Rule implements Serializable {
    private static final long serialVersionUID = 4188555156332337464L;

    private String m_addr1;
    private String m_addr2;
    private Long m_id;

    public EmailAddressPairRule() {
        this(null, null);
    }

    public EmailAddressPairRule(String addr1, String addr2) {
        m_addr1 = addr1;
        m_addr2 = addr2;
    }

    public void setAddress1(String addr1) {
        m_addr1 = addr1;
    }

    @Column(nullable=false)
    public String getAddress1() {
        return m_addr1;
    }

    public void setAddress2(String addr2) {
        m_addr2 = addr2;
    }

    @Column(nullable=false)
    public String getAddress2() {
        return m_addr2;
    }
}
