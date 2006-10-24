/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import com.metavize.mvvm.tran.PipelineEndpoints;

@MappedSuperclass
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="pl_endp_id", nullable=false)
    public PipelineEndpoints getPipelineEndpoints()
    {
        return pipelineEndpoints;
    }

    public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
    {
        this.pipelineEndpoints = pipelineEndpoints;
    }
}
