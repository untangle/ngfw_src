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

package com.untangle.uvm.engine;

import com.untangle.uvm.RemoteAppServerManager;
import com.untangle.uvm.security.CertInfo;
import com.untangle.uvm.security.RFC2253Name;

class RemoteAppServerManagerImpl implements RemoteAppServerManager
{
    private final AppServerManagerImpl lasm;

    RemoteAppServerManagerImpl(AppServerManagerImpl lasm)
    {
        this.lasm = lasm;
    }

    public boolean regenCert(RFC2253Name dn, int durationInDays)
    {
        return lasm.regenCert(dn, durationInDays);
    }

    public boolean importServerCert(byte[] cert, byte[] caCert)
    {
        return lasm.importServerCert(cert, caCert);
    }


    public byte[] getCurrentServerCert()
    {
        return lasm.getCurrentServerCert();
    }

    public byte[] generateCSR()
    {
        return lasm.generateCSR();
    }

    public CertInfo getCertInfo(byte[] certBytes)
    {
        return lasm.getCertInfo(certBytes);
    }
}
