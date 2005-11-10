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

import java.io.IOException;

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

    protected abstract void doSyslog(Appendable a) throws IOException;

    public void appendSyslog(Appendable a) throws IOException
    {
        a.append("endpoints: ");
        a.append("sid=");
        a.append(Integer.toString(pipelineEndpoints.getSessionId()));
        a.append(", prot=");
        a.append(Short.toString(pipelineEndpoints.getProtocol()));
        a.append(", caddr=");
        a.append(pipelineEndpoints.getCClientAddr().getHostAddress());
        a.append(", cport=");
        a.append(Integer.toString(pipelineEndpoints.getCClientPort()));
        a.append(", saddr=");
        a.append(pipelineEndpoints.getSServerAddr().getHostAddress());
        a.append(", sport=");
        a.append(Integer.toString(pipelineEndpoints.getSServerPort()));

        a.append(" #");

        doSyslog(a);
    }
}
