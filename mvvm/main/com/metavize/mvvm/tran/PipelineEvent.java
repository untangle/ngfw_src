/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: PipelineEvent.java,v 1.2 2005/03/25 03:51:16 amread Exp $
 */

package com.metavize.mvvm.tran;

import com.metavize.mvvm.logging.LogEvent;

/**
 * A pipeline event. Logged at the end of the session.
 *
 * XXX should we ad for the event type (enum: CREATE, DESTORY)
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="MVVM_EVT_PIPELINE"
 * mutable="false"
 */
public class PipelineEvent extends LogEvent
{
    private PipelineInfo info;

    // constructors -----------------------------------------------------------

    public PipelineEvent() { }

    public PipelineEvent(PipelineInfo info)
    {
        this.info = info;
    }

    // accessors --------------------------------------------------------------

    /**
     * Pipeline info.
     *
     * @return the pipeline info.
     * @hibernate.many-to-one
     * column="PIPELINE_INFO"
     * cascade="all"
     */
    public PipelineInfo getPipelineInfo()
    {
        return info;
    }

    public void setPipelineInfo(PipelineInfo info)
    {
        this.info = info;
    }
}
