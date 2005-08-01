/*
 * Copyright (c) 2003,2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.protofilter;

import java.io.Serializable;

/**
 * Rule for proto filter patterns
 *
 * @author <a href="mailto:dmorris@metavize.com">Dirk Morris</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_PROTOFILTER_PATTERN"
 */
public class ProtoFilterPattern implements Serializable
{
    private static final long serialVersionUID = 3997166364141492555L;

    private Long id;
    private String protocol = "none";
    private String description = "None";
    private String category = "None";
    private String definition = "";
    private String quality = "Bad";
    private boolean blocked = false;
    private boolean alert = false;
    private boolean log = false;

    /**
     * Hibernate constructor
     */
    public ProtoFilterPattern() {}

    public ProtoFilterPattern(String protocol, String category, String description,
                              String definition,  String quality,
                              boolean blocked, boolean alert, boolean log)
    {
        if (4096 < definition.length()) {
            throw new IllegalArgumentException("definition too long:"
                                               + definition);
        }

        this.protocol = protocol;
        this.category = category;
        this.description = description;
        this.definition = definition;
        this.quality = quality;
        this.blocked = blocked;
        this.alert = alert;
        this.log = log;
    }

    /**
     * @hibernate.id
     * column="RULE_ID"
     * generator-class="native"
     */
    protected Long getId() { return id; }
    protected void setId(Long id) { this.id = id; }

    /**
     * Protocol name
     *
     * @hibernate.property
     * column="PROTOCOL"
     */
    public String getProtocol() { return this.protocol; }
    public void setProtocol(String s) { this.protocol = s; }

    /**
     * Description name
     *
     * @hibernate.property
     * column="DESCRIPTION"
     */
    public String getDescription() { return this.description; }
    public void setDescription(String s) { this.description = s; }

    /**
     * Category of the rule
     *
     * @hibernate.property
     * column="CATEGORY"
     */
    public String getCategory() { return this.category; }
    public void setCategory(String s) { this.category = s; }

    /**
     * Definition (Regex) of the rule
     *
     * @hibernate.property
     * column="DEFINITION"
     * length="4096"
     */
    public String getDefinition() { return this.definition; }

    public void setDefinition(String s)
    {
        if (4096 < s.length()) {
            throw new IllegalArgumentException("argument too long:" + s);
        }

        this.definition = s;
    }

    /**
     * Flag that indicates if the traffic should be quality
     *
     * @hibernate.property
     * column="QUALITY"
     */
    public String getQuality() { return this.quality; }
    public void setQuality(String s) { this.quality = s; }

    /**
     * Flag that indicates if the traffic should be blocked
     *
     * @hibernate.property
     * column="BLOCKED"
     */
    public boolean isBlocked() { return this.blocked; }
    public void setBlocked(boolean b) { this.blocked = b; }

    /**
     * Should admin be alerted.
     *
     * @return true if alerts should be sent.
     * @hibernate.property
     * column="ALERT"
     */
    public boolean getAlert()
    {
        return alert;
    }
    public void setAlert(boolean alert)
    {
        this.alert = alert;
    }

    /**
     * Should admin be logged.
     *
     * @return true if should be logged.
     * @hibernate.property
     * column="LOG"
     */
    public boolean getLog()
    {
        return log;
    }
    public void setLog(boolean log)
    {
        this.log = log;
    }
}
