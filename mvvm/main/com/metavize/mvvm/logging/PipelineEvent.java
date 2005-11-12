/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;


import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.PipelineEndpoints;

public abstract class PipelineEvent extends LogEvent
{
    private PipelineEndpoints pipelineEndpoints;

    // constructors -----------------------------------------------------------

    public PipelineEvent() { }

    public PipelineEvent(int sessionId)
    {
        pipelineEndpoints = MvvmContextFactory.context().pipelineFoundry()
            .getPipeline(sessionId).getPipelineEndpoints();
    }

    // non-persistent accessors -----------------------------------------------

    /**
     * Get the PipelineEndpoints.
     *
     * @return the PipelineEndpoints.
     * @hibernate.many-to-one
     * column="PL_ENDP_ID"
     * not-null="true"
     * cascade="all"
     */
    public PipelineEndpoints getPipelineEndpoints()
    {
        return pipelineEndpoints;
    }

    public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
    {
        this.pipelineEndpoints = pipelineEndpoints;
    }

    public void setPipelineEndpoints(int sessionId)
    {
        pipelineEndpoints = MvvmContextFactory.context().pipelineFoundry()
            .getPipeline(sessionId).getPipelineEndpoints();
    }

    // Syslog methods ---------------------------------------------------------

    protected abstract void doSyslog(SyslogBuilder sb);

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.addField("sid", Integer.toString(pipelineEndpoints.getSessionId()));
        sb.addField("prot", Short.toString(pipelineEndpoints.getProtocol()));
        sb.addField("caddr", pipelineEndpoints.getCClientAddr().getHostAddress());
        sb.addField("cport", Integer.toString(pipelineEndpoints.getCClientPort()));
        sb.addField("saddr", pipelineEndpoints.getSServerAddr().getHostAddress());
        sb.addField("sport", Integer.toString(pipelineEndpoints.getSServerPort()));

        doSyslog(sb);
    }
}
