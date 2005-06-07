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

public interface Pipeline
{
    Long attach(Object o);
    Object getAttachment(Long key);
    Object detach(Long key);
    Fitting getClientFitting(MPipe mPipe);
    Fitting getServerFitting(MPipe mPipe);

    /**
     * Makes a temporary file that will be destroyed on Session
     * finalization.
     *
     * @return the temp file.
     * @exception IOException the temp file cannot be created.
     */
    File mktemp() throws IOException;
}
