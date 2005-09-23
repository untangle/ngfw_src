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

package com.metavize.tran.sigma;

import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the Sigma transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_PROTOFILTER_SETTINGS"
 */
public class SigmaSettings implements java.io.Serializable
{
    private static final long serialVersionUID = 266434887860496780L;

    private Long id;
    private Tid tid;
    private int bufferSize = 4096;
    private int byteLimit  = 2048;
    private int chunkLimit = 8;
    private String unknownString = "[unknown]";
    private boolean stripZeros = true;
    private List patterns = null;

    /**
     * Hibernate constructor.
     */
    private SigmaSettings() {}

    /**
     * Real constructor
     */
    public SigmaSettings(Tid tid)
    {
        this.tid = tid;
        this.patterns = new ArrayList();
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

    /**
     * Strip zeros from data before scanning
     *
     * @hibernate.property
     * column="MYSETTING"
     */
    public boolean isMySetting() {return this.stripZeros;}
    public void setMySetting(boolean b) {this.stripZeros = b;}
}
