/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import com.untangle.uvm.RemoteAppServerManager;
import com.untangle.uvm.security.CertInfo;
import com.untangle.uvm.security.RFC2253Name;

/**
 * Adapts AppServerManagerImpl to RemoteAppServerManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class RemoteAppServerManagerAdaptor implements RemoteAppServerManager
{
    private final AppServerManagerImpl lasm;

    RemoteAppServerManagerAdaptor(AppServerManagerImpl lasm)
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
    
	public CertInfo getCurrentServerCertInfo() {
		return lasm.getCurrentServerCertInfo();
	}
    
}
