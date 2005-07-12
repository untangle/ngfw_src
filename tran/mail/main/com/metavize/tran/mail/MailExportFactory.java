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

package com.metavize.tran.mail;

import com.metavize.mvvm.tapi.ProxyGenerator;

public class MailExportFactory
{
    private static MailExport transform = null;

    public static void init(MailExport transform)
    {
        if (null != MailExportFactory.transform) {
            throw new IllegalStateException("already initialized");
        }

        MailExportFactory.transform = (MailExport)ProxyGenerator
            .generateProxy(MailExport.class, transform);
    }

    public static MailExport getTransform()
    {
        return transform;
    }
}
