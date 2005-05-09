/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm;

import java.io.IOException;

import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.tran.TransformManager;
import com.metavize.mvvm.ArgonManager;

/**
 * Provides an interface to get major MVVM components from
 * an MVVM instance.  This interface is accessible both locally
 * and remotely.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface MvvmContext
{
    /**
     * Get the <code>ToolboxManager</code> singleton.
     *
     * @return a <code>ToolboxManager</code> value
     */
    ToolboxManager toolboxManager();

    /**
     * Get the <code>TransformManager</code> singleton.
     *
     * @return a <code>TransformManager</code> value
     */
    TransformManager transformManager();

    /**
     * Get the <code>LoggingManager</code> singleton.
     *
     * @return a <code>LoggingManager</code> value
     */
    LoggingManager loggingManager();

    /**
     * Get the <code>MailSender</code> singleton.
     *
     * @return a <code>MailSender</code> value
     */
    MailSender mailSender();

    /**
     * Get the <code>AdminManager</code> singleton.
     *
     * @return a <code>AdminManager</code> value
     */
    AdminManager adminManager();

    /**
     * Get the <code>NetworkingManager</code> singleton.
     *
     * @return a <code>NetworkingManager</code> value
     */
    NetworkingManager networkingManager();

    /**
     * Retrieve the argon manager.
     */
    ArgonManager argonManager();

    /**
     * Save settings to local hard drive.
     *
     * @exception IOException if the save was unsuccessful.
     */
    void localBackup() throws IOException;

    /**
     * Save settings to USB key drive.
     *
     * @exception IOException if the save was unsuccessful.
     */
    void usbBackup() throws IOException;

    // lifecycle --------------------------------------------------------------
    void shutdown();

    // debugging / performance management
    void doFullGC();
}
