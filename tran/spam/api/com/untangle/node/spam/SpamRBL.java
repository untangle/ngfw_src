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

package com.untangle.node.spam;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

/**
 * spam RBL
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_spam_rbl", schema="settings")
public class SpamRBL implements Serializable
{
    private static final long serialVersionUID = -7246008133224041234L;

    // a spam RBL list with a single NO_RBL hostname will deactivate RBL and
    // prevent the list from re-initializing with default hostnames
    // or
    // a spam RBL list with active=false for all entries will deactivate RBL and
    // prevent the list from re-initializing with default values
    public static final String NO_RBL = "127.0.0.1";
    public static final String NO_RBL_DESCRIPTION = "Spam RBL deactivated";

    public static final String EMPTY_DESCRIPTION = "[no description]";

    private Long id;

    private String hostname;
    private String description = EMPTY_DESCRIPTION;
    private boolean active = false;

    // constructors -----------------------------------------------------------

    public SpamRBL() {}

    public SpamRBL(String hostname, String description, boolean active) {
        this.hostname = hostname;
        this.description = description;
        this.active = active;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="id")
    @GeneratedValue
    private Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    /**
     * hostname of spam RBL
     *
     * @return hostname of spam RBL
     */
    @Column(nullable=false)
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
        return;
    }

    /**
     * description of hostname (for display)
     *
     * @return description of hostname
     */
    @Column(nullable=true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        return;
    }

    /**
     * active spam RBL flag
     *
     * @return active spam RBL flag
     */
    @Column(nullable=false)
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        return;
    }
}
