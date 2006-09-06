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

package com.metavize.mvvm.networking;

import java.util.List;

import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.IPaddr;

public interface DnsServerSettings
{
    // !!!!! private static final long serialVersionUID = 4349679825783697834L;
    /**
     * If DNS Masquerading is enabled.
     */
    public boolean getDnsEnabled();

    public void setDnsEnabled( boolean b );

    /**
     * Local Domain
     */
    public HostName getDnsLocalDomain();

    public void setDnsLocalDomain( HostName s );

    /**
     * List of the DNS Static Host rules.
     */
    public List<DnsStaticHostRule> getDnsStaticHostList();

    public void setDnsStaticHostList( List<DnsStaticHostRule> s );
}
