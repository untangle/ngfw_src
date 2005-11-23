/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareSettings.java 3501 2005-11-21 10:12:33Z amread $
 */

package com.metavize.tran.ids;

import com.metavize.mvvm.tran.Rule;

import java.io.Serializable;

/**
 * Hibernate object to store IDS rules.
 *
 * @author <a href="mailto:nchilders@metavize.com">Nick Childers</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_IDS_RULE"
 */

public class IDSRule extends Rule implements Serializable {
    private static final long serialVersionUID = -7009708957041660234L;
	
    //Hibernate Variables
    private String rule;

    private int sid;

    // Used from UI to let us know it's changed.
    private boolean modified;
	
    //Variables set at run time
    private transient IDSRuleHeader header;
    private transient IDSRuleSignature signature;
    private transient boolean remove; //Should no longer be in the list

    /**
     * Hibernate constructor
     */
    public IDSRule() {}

    public IDSRule(String rule, String  category, String description) {

        super("Name", category,description,false);
		
        if(4096 < rule.length())
            throw new IllegalArgumentException("definition too long:" + rule);

        this.rule = rule;
        this.modified = false;
        this.remove = false;
    }
	
    public long getKeyValue() { return super.getId(); }
    public void setKeyValue(Long val) { super.setId(val); }

    public boolean getModified() { return modified; }
    public void setModified(boolean val) { modified = val; }
	
    /**
     * @hibernate.property
     * column="RULE"
     * length="4096"
     */
    public String getText() { return this.rule; }
    public void setText(String s) { this.rule = s; }	

    /**
     * @hibernate.property
     * column="SID"
     */
    public int getSid() { return this.sid; }
    public void setSid(int sid) { this.sid = sid; }	

    //Non Hibernate functions
    public void setHeader(IDSRuleHeader header) {
        this.header = header;
    }
	
    public IDSRuleHeader getHeader() {
        return header;
    }

    public void setSignature(IDSRuleSignature signature) {
        this.signature = signature;
        //super.setDescription(getMessage());
    }

    public IDSRuleSignature getSignature() {
        return signature;
    }

    public boolean remove() {
        return remove;
    }

    public void remove(boolean val) {
        remove = val;
    }

    public boolean disabled() {
        return !(isLive() || getLog());
    }

    public int getAction() {
        if (isLive())
            return IDSRuleManager.BLOCK;
        else if (getLog())
            return IDSRuleManager.LOG;
        else
            // XX
            return IDSRuleManager.ALERT;
    }

    public boolean equals(Object o) {
        if (o instanceof IDSRule) {
            IDSRule other = (IDSRule) o;
            // Following isn't totally complete, but is good enough for what we use from Rule. XX
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
}
