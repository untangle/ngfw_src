package com.metavize.tran.ids;

import java.io.Serializable;

/**
 * Hibernate object to store IDS rules.
 *
 * @author <a href="mailto:nchilders@metavize.com">Nick Childers</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_IDS_RULE"
 */

public class IDSRule implements Serializable {
	private static final long serialVersionUID = -7009708957041660234L;
	private Long id;

	private boolean on;
	private boolean alert;
	private boolean blocked;
	private boolean log;
	private String rule;

	/**
	 * Hibernate constructor
	 */
	public IDSRule() {}

	public IDSRule(String rule) {

		if(4096 < rule.length())
			throw new IllegalArgumentException("definition too long:" + rule);

		this.rule = rule;
	}

	/**
	 * @hibernate.id
	 * column="RULE_ID"
	 * generator-class="native"
	 */

	protected Long getID() { return id; }
	protected void setID(Long id) { this.id = id; }

	/**
	 * @hibernate.property
	 * column="RULE"
	 * length="4096"
	 */

	public String getRule() { return this.rule; }
	public void setRule(String s) { this.rule = s; }		
}
