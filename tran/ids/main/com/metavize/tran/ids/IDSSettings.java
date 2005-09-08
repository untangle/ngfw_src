package com.metavize.tran.ids;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import com.metavize.mvvm.tran.StringRule;
import com.metavize.mvvm.security.Tid;

/**
 * Hibernate object to store IDS settings.
 *
 * @author <a href="mailto:nchilders@metavize.com">Nick Childers</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_IDS_SETTINGS"
 */

public class IDSSettings implements Serializable {
	private static final long serialVersionUID = -7056565971726289302L;
	private int maxChunks;
	private Long id;
	private Tid tid;
	private List rules = new ArrayList();
	private List variables = new ArrayList();
	private List immutableVariables = new ArrayList();

	/**
	 * Hibernate constructor
	 */
	public IDSSettings() {}

	public IDSSettings(Tid tid) {
		this.tid = tid;
	}

	/**
	 * @hibernate.id
	 * column="SETTINGS_ID"
	 * generator-class="native"
	 */
	protected Long getID() { return id; }
	protected void setID(Long id) { this.id = id; }

	/**
	 * @hibernate.property
	 * column="MAX_CHUNKS"
	 */

	protected int getMaxChunks() { return maxChunks; }
	protected void setMaxChunks(int maxChunks) { this.maxChunks = maxChunks; }
		
	/**
	 * Transform id for these settings.
	 *
	 * @return tid for these settings.
	 * @hibernate.many-to-one
	 * column="TID"
	 * unique="true"
	 * not-null="true"
	 */
	    public Tid getTid() {
			return tid;
		}
		
		public void setTid(Tid tid) {
			this.tid = tid;
		}
			
	/**
	 * @hibernate.list
	 * cascade="all-delete-orphan"
	 * @hibernate.collection-key
	 * column="SETTINGS_ID"
	 * @hibernate.collection-index
	 * column="POSITION"
	 * @hibernate.collection-one-to-many
	 * class="com.metavize.tran.ids.IDSRule"
	 */
	public List getRules() { return this.rules; }
	public void setRules(List rules) { this.rules = rules; }	

	/**
	 * @hibernate.list
	 * cascade="all-delete-orphan"
	 * table="TR_IDS_MUTABLE_VARIABLES"
	 * @hibernate.collection-key
	 * column="SETTING_ID"
	 * @hibernate.collection-index
	 * column="POSITION"
	 * @hibernate.collection-many-to-many
	 * class="com.metavize.tran.ids.IDSVariable"
 	 * column="VARIABLE_ID"
	 */

	public List getVariables() { return this.variables;	}
	public void setVariables(List variables) { this.variables = variables; }

	/**
 	 * @hibernate.list
 	 * cascade="all-delete-orphan"
	 * table="TR_IDS_IMMUTABLE_VARIABLES"
 	 * @hibernate.collection-key
 	 * column="SETTING_ID"
 	 * @hibernate.collection-index
 	 * column="POSITION"
 	 * @hibernate.collection-many-to-many
 	 * class="com.metavize.tran.ids.IDSVariable"
 	 * column="VARIABLE_ID"
	 */

	 public List getImmutableVariables() { return this.immutableVariables; }
	 public void setImmutableVariables(List variables) { this.immutableVariables = variables; }

			
}
