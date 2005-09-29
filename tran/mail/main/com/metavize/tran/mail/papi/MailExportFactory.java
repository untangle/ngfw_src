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

package com.metavize.tran.mail.papi;


import com.metavize.mvvm.tapi.ProxyGenerator;
import org.apache.log4j.Logger;

/**
 * Factory for the exported MailTransform interface.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class MailExportFactory
{
    private static final MailExportFactory FACTORY = new MailExportFactory();

    private static final Object LOCK = new Object();

    private final Logger logger = Logger.getLogger(MailExportFactory.class);

    private MailExport export;

    // constructors -----------------------------------------------------------

    private MailExportFactory() { }

    // static factories -------------------------------------------------------

    public static MailExportFactory factory()
    {
        return FACTORY;
    }

    // public methods ---------------------------------------------------------

    /**
     * Allows the casing to export its interface for its policy.
     *
     * @param policy policy for these Exports.
     * @param export exported interface.
     */
    public void registerExport(MailExport export)
    {
        synchronized (LOCK) {
            if (null != export) {
                logger.warn("replacing export");
            }

            export = (MailExport)ProxyGenerator.generateProxy(MailExport.class,
                                                              export);
        }
    }

    /**
     * Gets the exported interface for a given policy.
     *
     * @param policy the policy of the exported interface.
     * @return the exported interface.
     */
    public MailExport getExport()
    {
        synchronized (LOCK) {
            return export;
        }
    }
}
