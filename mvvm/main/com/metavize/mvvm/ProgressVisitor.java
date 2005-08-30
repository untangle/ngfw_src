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

package com.metavize.mvvm;

public interface ProgressVisitor
{
    public void visitDownloadProgress(DownloadProgress dp);
    public void visitDownloadComplete(DownloadComplete dc);
    public void visitInstallComplete(InstallComplete ic);
    public void visitInstallTimeout(InstallTimeout it);
}
