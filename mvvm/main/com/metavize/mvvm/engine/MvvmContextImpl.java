/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: MvvmContextImpl.java,v 1.12 2005/03/23 07:05:29 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.io.IOException;

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MvvmContext;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.ToolboxManager;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.tran.TransformManager;
import org.apache.log4j.Logger;

public abstract class MvvmContextImpl extends MvvmContextBase
    implements MvvmContext
{
    private static final String BACKUP_SCRIPT
        = System.getProperty("bunnicula.home") + "/../../bin/mvvmdb-backup";
    private static final String LOCAL_ARG = "local";
    private static final String USB_ARG = "usb";

    private static final Logger logger = Logger
        .getLogger(MvvmContextImpl.class.getName());

    protected AdminManagerImpl adminManager;
    protected LoggingManagerImpl loggingManager;
    protected MailSenderImpl mailSender;
    protected ToolboxManagerImpl toolboxManager;
    protected TransformManagerImpl transformManager;
    protected NetworkingManager networkingManager;

    protected MvvmContextImpl() { }

    public ToolboxManager toolboxManager()
    {
        return toolboxManager;
    }

    public TransformManager transformManager()
    {
        return transformManager;
    }

    public LoggingManager loggingManager()
    {
        return loggingManager;
    }

    public MailSender mailSender()
    {
        return mailSender;
    }

    public AdminManager adminManager()
    {
        return adminManager;
    }

    public NetworkingManager networkingManager()
    {
        return networkingManager;
    }

    public void localBackup() throws IOException
    {
        backup(true);
    }

    public void usbBackup() throws IOException
    {
        backup(false);
    }

    private void backup(boolean local) throws IOException
    {
        Process p = Runtime.getRuntime().exec(new String[]
            { BACKUP_SCRIPT, local ? LOCAL_ARG : USB_ARG });
        for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );


        while (true) {
            try {
                int exitValue = p.waitFor();
                if (0 != exitValue) {
                    throw new IOException("dump not successful");
                } else {
                    return;
                }
            } catch (InterruptedException exn) { }
        }
    }

    // lifecycle --------------------------------------------------------------
    public void shutdown()
    {
        // XXX check access permission

        new Thread(new Runnable()
            {
                public void run()
                {
                    try {
                        Thread.currentThread().sleep(200);
                    } catch (InterruptedException exn) { }
                    logger.info("thank you for choosing bunnicula");
                    System.exit(0);
                }
            }).start();
    }
}
