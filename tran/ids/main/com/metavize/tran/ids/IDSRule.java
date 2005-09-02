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
	private String rule;

	private boolean modified;

	/**
	 * Hibernate constructor
	 */
	public IDSRule() {}

	public IDSRule(String rule, String  category, String description) {

		super("Name", category,description,false);
		
		if(4096 < rule.length())
			throw new IllegalArgumentException("definition too long:" + rule);

		this.rule = rule;
		this.modified = true;
	}
	
	/**
	 * @hibernate.property
	 * column="MODIFIED"
	 */
	
	public boolean getModified() {
		return modified;
	}

	public void setModified(boolean val) {
		modified = val;
	}
	
	/**
	 * @hibernate.property
	 * column="RULE"
	 * length="4096"
	 */

	public String getRule() { return this.rule; }
	public void setRule(String s) { this.rule = s; }	

	public void setLive(boolean live) { 
		super.setLive(live);
		//System.out.println("SetLive is being called");
	}
}
