package com.untangle.node.cpd;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Block event for the captive portal.  This is for each session that 
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_cpd_block_evt", schema="events")
public class BlockEvent extends PipelineEvent implements Serializable
{
	private static final long serialVersionUID = 5966627183254560467L;

	// Constructors
	public BlockEvent() { }

	public BlockEvent( PipelineEndpoints pe )
	{
		super(pe);
	}

	// Syslog methods -----------------------------------------------------
	public void appendSyslog(SyslogBuilder sb)
	{
		getPipelineEndpoints().appendSyslog(sb);

		sb.startSection("info");
	}

	@Transient
	public String getSyslogId()
	{
		return "cpd-block";
	}

	@Transient
	public SyslogPriority getSyslogPriority()
	{
		// INFORMATIONAL = statistics or normal operation
		// WARNING = traffic altered
		return SyslogPriority.INFORMATIONAL;
	}
}
