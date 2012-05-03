/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;

/**
 * Abstract class for rules.
 */
public abstract class Rule implements Serializable
{
    public static final String EMPTY_NAME        = "[no name]";
    public static final String EMPTY_DESCRIPTION = "[no description]";
    public static final String EMPTY_CATEGORY    = "[no category]";

    private Long id;
    private String name = EMPTY_NAME;
    private String category = EMPTY_CATEGORY;
    private String description = EMPTY_DESCRIPTION;
    // XXX we need to set hibernate & SQL NOT NULL on these
    private boolean live = true;
    private boolean alert = false;
    private boolean log = false;

    // constructors -----------------------------------------------------------

	public Rule() { }

    public Rule(boolean live)
    {
        this.live = live;
    }

    public Rule(boolean live, String name)
    {
        this.live = live;
        this.name = name;
    }

    public Rule(String name, String category, String description, boolean live)
    {
        this.live = live;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public Rule(String name, String category, String description)
    {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public Rule(String name, String category)
    {
        this.name = name;
        this.category = category;
    }

    public Rule(String name, String category, boolean live)
    {
        this.name = name;
        this.category = category;
        this.live = live;
    }

    
    // accessors --------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isLive() { return live; }
    public void setLive(boolean live) { this.live = live; }

    public boolean getAlert() { return alert; }
    public void setAlert(boolean alert) { this.alert = alert; }

    public boolean getLog() { return log; }
    public void setLog(boolean log) { this.log = log; }
    
    public void update(Rule rule) {
		this.name = rule.name;
		this.category = rule.category;
		this.description = rule.description;
		this.live = rule.live;
		this.alert = rule.alert;
		this.log = rule.log;
	}

}
