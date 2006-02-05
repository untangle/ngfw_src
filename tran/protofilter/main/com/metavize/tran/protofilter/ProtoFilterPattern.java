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
    // Converter sets all old patterns to this mid
    public static final int NEEDS_CONVERSION_METAVIZE_ID = -27;

    // All user owned rules should have this value
    public static final int USER_CREATED_METAVIZE_ID = 0;

    private static final long serialVersionUID = 3997166364141492555L;

    private Long id;
    private int mvid = USER_CREATED_METAVIZE_ID;
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

    ProtoFilterPattern(int mvid, String protocol, String category, String description,
                       String definition,  String quality,
                       boolean blocked, boolean alert, boolean log)
    {
        this.mvid = mvid;
        this.protocol = protocol;
        this.category = category;
        this.description = description;
        this.definition = definition;
        this.quality = quality;
        this.blocked = blocked;
        this.alert = alert;
        this.log = log;
    }

    // For use by UI
    public boolean isReadOnly() {
        return (mvid == USER_CREATED_METAVIZE_ID);
    }

    /**
     * @hibernate.id
     * column="RULE_ID"
     * generator-class="native"
     */
    protected Long getId() { return id; }
    protected void setId(Long id) { this.id = id; }

    /**
     *
     * Note that metavize id should not be set by the user.  It is only ever set for
     * Metavize built-in patterns.
     *
     * @hibernate.property
     * column="METAVIZE_ID"
     */
    public int getMetavizeId() { return mvid; }
    public void setMetavizeId(int mvid) { this.mvid = mvid; }

    // For UI
    public void setMetavizeId(Integer mvid) { this.mvid = mvid.intValue(); }

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
     */
    public String getDefinition() { return this.definition; }

    public void setDefinition(String s)
    {
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
