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

package com.untangle.node.ips;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.Rule;

/**
 * Hibernate object to store Ips rules.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@Table(name="n_ips_rule", schema="settings")
@SuppressWarnings("serial")
public class IpsRule extends Rule implements Serializable
{

    // Actions (indices to ACTIONS)
    public static final int ALERT = 0;
    public static final int LOG = 1;
    public static final int PASS = 2;
    public static final int BLOCK = 3;

    public static final String[] ACTIONS = { "alert","log","pass","block" };

    //Hibernate Variables
    private String rule;

    private int sid;

    // Used from UI to let us know it's changed.
    private boolean modified;

    private String classification;
    private String url;

    public IpsRule() {}

    public IpsRule(String rule, String  category, String description) {

        super("Name", category,description,false);

        this.rule = rule;
        this.modified = false;
    }

    @Transient
    public long getKeyValue() { return getId(); }
    public void setKeyValue(Long val) { setId(val); }

    @Transient
    public boolean getModified() { return modified; }
    public void setModified(boolean val) { modified = val; }

    @Column(name="rule")
    public String getText() { return this.rule; }
    public void setText(String s) { this.rule = s; }

    @Column(nullable=false)
    public int getSid() { return this.sid; }
    public void setSid(int sid) { this.sid = sid; }

    /* every rule signature has classification (so default text is replaced) */
    @Transient
    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
        return;
    }

    /* not all rule signatures have url (so default text may be returned) */
    @Transient
    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
        return;
    }

    public boolean disabled() {
        return !(isLive() || getLog());
    }

    @Transient
    public int getAction() {
        if (isLive())
            return BLOCK;
        else if (getLog())
            return LOG;
        else
            // XX
            return ALERT;
    }

    public boolean equals(Object o) {
        if (o instanceof IpsRule) {
            IpsRule other = (IpsRule) o;
            // Following isn't totally complete, but is good enough
            // for what we use from Rule. XX
            return (rule == null ? other.rule == null : rule.equals(other.rule)) &&
                isLive() == other.isLive() &&
                getLog() == other.getLog();
        }
        return false;
    }

    public int hashCode()
    {
        // Good enough. XX
        return (null == rule ? 0 : rule.hashCode());
    }

    public void update(IpsRule rule) {
	super.update(rule);
	this.rule = rule.rule;
	this.sid = rule.sid;
    }
    
}
