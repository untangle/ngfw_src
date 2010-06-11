/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.protofilter;

import java.io.Serializable;
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

/**
 * Rule for proto filter patterns
 *
 * @author <a href="mailto:dmorris@untangle.com">Dirk Morris</a>
 * @version 1.0
 */
@Entity
@Table(name="n_protofilter_pattern", schema="settings")
public class ProtoFilterPattern implements Serializable
{
    // Converter sets all old patterns to this mid
    public static final int NEEDS_CONVERSION_METAVIZE_ID = -27;

    // All user owned rules should have this value
    public static final int USER_CREATED_METAVIZE_ID = 0;


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

    public ProtoFilterPattern() { }

    ProtoFilterPattern(int mvid, String protocol, String category,
                       String description, String definition,  String quality,
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

    @Transient
    public boolean isReadOnly() {
        return (mvid == USER_CREATED_METAVIZE_ID);
    }

    @Id
    @Column(name="rule_id")
    @GeneratedValue
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /**
     *
     * Note that metavize id should not be set by the user.  It is
     * only ever set for Metavize built-in patterns.
     */
    @Column(name="metavize_id")
    public int getMetavizeId() { return mvid; }
    public void setMetavizeId(int mvid) { this.mvid = mvid; }

    @Transient
    public void setMetavizeId(Integer mvid) { this.mvid = mvid.intValue(); }

    /**
     * Protocol name
     */
    public String getProtocol() { return this.protocol; }
    public void setProtocol(String s) { this.protocol = s; }

    /**
     * Description name
     */
    public String getDescription() { return this.description; }
    public void setDescription(String s) { this.description = s; }

    /**
     * Category of the rule
     */
    public String getCategory() { return this.category; }
    public void setCategory(String s) { this.category = s; }

    /**
     * Definition (Regex) of the rule
     */
    public String getDefinition() { return this.definition; }

    public void setDefinition(String s)
    {
        this.definition = s;
    }

    /**
     * Flag that indicates if the traffic should be quality
     */
    public String getQuality() { return this.quality; }
    public void setQuality(String s) { this.quality = s; }

    /**
     * Flag that indicates if the traffic should be blocked
     */
    public boolean isBlocked() { return this.blocked; }
    public void setBlocked(boolean b) { this.blocked = b; }

    /**
     * Should admin be alerted.
     *
     * @return true if alerts should be sent.
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
     */
    public boolean getLog()
    {
        return log;
    }
    public void setLog(boolean log)
    {
        this.log = log;
    }

    public void updateRule(ProtoFilterPattern pattern) {
        this.protocol = pattern.protocol;
        this.category = pattern.category;
        this.description = pattern.description;
        this.definition = pattern.definition;
        this.blocked = pattern.blocked;
        this.alert = pattern.alert;
        this.log = pattern.log;
    }
}
