/**
 * $Id$
 */
package com.untangle.uvm.logging;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.untangle.uvm.node.PipelineEndpoints;

/**
 * A <code>LogEvent</code> that has <code>PipelineEndpoints<code>.
 */
@SuppressWarnings("serial")
@MappedSuperclass
public abstract class PipelineEvent extends LogEvent
{
    private PipelineEndpoints pipelineEndpoints;

    public PipelineEvent() { }

    public PipelineEvent(PipelineEndpoints pe)
    {
        pipelineEndpoints = pe;
    }

    /**
     * Get the session Id
     *
     * @return the the session Id
     */
    @Column(name="session_id", nullable=false)
    public Integer getSessionId()
    {
        return pipelineEndpoints.getSessionId();
    }

    public void setSessionId( Integer sessionId )
    {
        this.pipelineEndpoints.setSessionId(sessionId);
    }

    @Transient
    public PipelineEndpoints getPipelineEndpoints()
    {
        return pipelineEndpoints;
    }

    public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
    {
        this.pipelineEndpoints = pipelineEndpoints;
    }
}
