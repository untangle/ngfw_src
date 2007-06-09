/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.ftp;

public interface FtpTransform
{
    FtpSettings getFtpSettings();
    void setFtpSettings(FtpSettings settings);
}
