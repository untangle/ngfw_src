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

package com.metavize.mvvm.tapi;

import java.io.File;
import java.io.IOException;

import com.metavize.mvvm.tran.PipelineEndpoints;

public interface Pipeline
{
    Long attach(Object o);
    Object getAttachment(Long key);
    Object detach(Long key);
    Fitting getClientFitting(MPipe mPipe);
    Fitting getServerFitting(MPipe mPipe);

    PipelineEndpoints getPipelineEndpoints();

    /**
     * Makes a temporary file that will be destroyed on Session
     * finalization.
     *
     * @return the temp file.
     * @exception IOException the temp file cannot be created.
     */
    File mktemp() throws IOException;

    /**
     * Makes a temporary file that will be destroyed on Session
     * finalization.  The file name will start with the given prefix
     * (for debugging purposes).
     *
     * NOTE: the prefix <b>must not</b> come from user data, it should
     * be a constant like 'ftp-virus'.
     *
     * @return the temp file.
     * @exception IOException the temp file cannot be created.
     */
    File mktemp(String prefix) throws IOException;
}
