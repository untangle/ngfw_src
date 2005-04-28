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

package com.metavize.tran.protofilter;

import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the ProtoFilter transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_PROTOFILTER_SETTINGS"
 */
public class ProtoFilterSettings implements java.io.Serializable
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
    private ProtoFilterSettings() {}

    /**
     * Real constructor
     */
    public ProtoFilterSettings(Tid tid)
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
     * unique="true"
     * not-null="true"
     */
    public Tid getTid() {return tid;}
    public void setTid(Tid tid) {this.tid = tid;}

    /**
     * Max buffer size for scanning
     *
     * @hibernate.property
     * column="BUFFERSIZE"
     */
    public int getBufferSize() {return this.bufferSize;}
    public void setBufferSize(int i) {this.bufferSize = i;}

    /**
     * Byte limit for scanning
     *
     * @hibernate.property
     * column="BYTELIMIT"
     */
    public int getByteLimit() {return this.byteLimit;}
    public void setByteLimit(int i) {this.byteLimit = i;}

    /**
     * Chunk limit for scanning
     *
     * @hibernate.property
     * column="CHUNKLIMIT"
     */
    public int getChunkLimit() {return this.chunkLimit;}
    public void setChunkLimit(int i) {this.chunkLimit = i;}

    /**
     * Unknown string for unknown matches
     *
     * @hibernate.property
     * column="UNKNOWNSTRING"
     */
    public String getUnknownString() {return this.unknownString;}
    public void setUnknownString(String s) {this.unknownString = s;}

    /**
     * Strip zeros from data before scanning
     *
     * @hibernate.property
     * column="STRIPZEROS"
     */
    public boolean isStripZeros() {return this.stripZeros;}
    public void setStripZeros(boolean b) {this.stripZeros = b;}

    /**
     * Pattern rules.
     *
     * @return the list of Patterns
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.protofilter.ProtoFilterPattern"
     */
    public List getPatterns() {return patterns;}
    public void setPatterns(List s) {this.patterns = s;}
}
