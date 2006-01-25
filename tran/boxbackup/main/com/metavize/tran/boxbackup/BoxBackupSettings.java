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

package com.metavize.tran.boxbackup;

import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the BoxBackup transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_PROTOFILTER_SETTINGS"
 */
public class BoxBackupSettings implements java.io.Serializable
{
    private static final long serialVersionUID = 266434887860496780L;

    private Long id;
    private Tid tid;

    /**
     * Hibernate constructor.
     */
    private BoxBackupSettings() {}

    /**
     * Real constructor
     */
    public BoxBackupSettings(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
    private Long getId() {return id;}
    private void setId(Long id) {this.id = id;}

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * not-null="true"
     */
    public Tid getTid() {return tid;}
    public void setTid(Tid tid) {this.tid = tid;}
}
