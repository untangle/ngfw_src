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
    private static MailExport export = null;

    public static void init(MailExport export)
    {
        if (null != MailExportFactory.export) {
            throw new IllegalStateException("already initialized");
        }

        MailExportFactory.export = (MailExport)ProxyGenerator
            .generateProxy(MailExport.class, export);
    }

    public static MailExport getExport()
    {
        return export;
    }
}
