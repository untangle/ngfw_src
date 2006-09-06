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

import com.metavize.mvvm.tran.PipelineEndpoints;

public abstract class PipelineEvent extends LogEvent
{
    private PipelineEndpoints pipelineEndpoints;

    // constructors -----------------------------------------------------------

    public PipelineEvent() { }

    public PipelineEvent(PipelineEndpoints pe)
    {
        pipelineEndpoints = pe;
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
}
