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

import java.util.Map;
import java.util.WeakHashMap;

import com.metavize.mvvm.policy.Policy;
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

    private final Map<Policy, MailExport> exports;
    private final Logger logger = Logger.getLogger(MailExportFactory.class);

    // constructors -----------------------------------------------------------

    private MailExportFactory()
    {
        exports = new WeakHashMap<Policy, MailExport>();
    }

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
    public void registerExport(Policy policy, MailExport export)
    {
        synchronized (exports) {
            if (exports.containsKey(policy)) {
                logger.warn("replacing export for policy: " + policy);
            }

            MailExport pxy = (MailExport)ProxyGenerator
                .generateProxy(MailExport.class, export);
            exports.put(policy, pxy);
        }
    }

    /**
     * Gets the exported interface for a given policy.
     *
     * @param policy the policy of the exported interface.
     * @return the exported interface.
     */
    public MailExport getExport(Policy policy)
    {
        synchronized (exports) {
            return exports.get(policy);
        }
    }
}
