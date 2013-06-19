/**
 * $Id$
 */
package com.untangle.node.ips;

import java.io.Serializable;

/**
 * Hibernate object to store Ips rules.
 */
@SuppressWarnings("serial")
public class IpsRule implements Serializable
{
    public static final String EMPTY_NAME        = "[no name]";
    public static final String EMPTY_DESCRIPTION = "[no description]";
    public static final String EMPTY_CATEGORY    = "[no category]";

    private Long id;
    private String name = EMPTY_NAME;
    private String category = EMPTY_CATEGORY;
    private String description = EMPTY_DESCRIPTION;
    private boolean live = true;
    private boolean alert = false;
    private boolean log = false;

    // Actions (indices to ACTIONS)
    public static final int ALERT = 0;
    public static final int LOG = 1;
    public static final int PASS = 2;
    public static final int BLOCK = 3;

    public static final String[] ACTIONS = { "alert","log","pass","block" };

    private String classification;
    private String rule;
    private String url;
    private int sid;

    public IpsRule() {}

    public IpsRule(String rule, String  category, String description)
    {
        this.rule = rule;
        this.name = "Name";
        this.category = category;
        this.description = description;
        this.live = false;
    }

    public Long getId() { return id; }
    public void setId( Long id ) { this.id = id; }

    /**
     * Get a name for display purposes.
     */
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    /**
     * Get a category for display purposes.
     */
    public String getCategory() { return category; }
    public void setCategory( String category ) { this.category = category; }

    /**
     * Get a description for display purposes.
     */
    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    /**
     * Will the rule be used for matching?
     */
    public boolean isLive() { return live; }
    public void setLive( boolean live ) { this.live = live; }

    /**
     * Should rule be logged
     */
    public boolean getLog() { return log; }
    public void setLog( boolean log ) { this.log = log; }
    
    public long trans_getKeyValue() { return getId(); }
    public void trans_setKeyValue(Long val) { setId(val); }

    public String getText() { return this.rule; }
    public void setText(String s) { this.rule = s; }

    public int getSid() { return this.sid; }
    public void setSid(int sid) { this.sid = sid; }

    /* every rule signature has classification (so default text is replaced) */
    public String trans_getClassification() { return classification; }
    public void trans_setClassification(String classification) { this.classification = classification; }

    /* not all rule signatures have url (so default text may be returned) */
    public String trans_getURL() { return url; }
    public void trans_setURL(String url) { this.url = url; return; }

    public boolean disabled() { return !(isLive() || getLog()); }

    public int trans_getAction()
    {
        if (isLive())
            return BLOCK;
        else if (getLog())
            return LOG;
        else
            // XX
            return ALERT;
    }

    public boolean equals(Object o)
    {
        if (o instanceof IpsRule)
        {
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

    public void update(IpsRule rule)
    {
        this.name = rule.name;
        this.category = rule.category;
        this.description = rule.description;
        this.live = rule.live;
        this.alert = rule.alert;
        this.log = rule.log;
        this.rule = rule.rule;
        this.sid = rule.sid;
    }
}
