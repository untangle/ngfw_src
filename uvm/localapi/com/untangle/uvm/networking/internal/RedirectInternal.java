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

package com.untangle.mvvm.networking.internal;

import com.untangle.mvvm.networking.RedirectRule;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.firewall.ip.IPDBMatcher;
import com.untangle.mvvm.tran.firewall.port.PortDBMatcher;
import com.untangle.mvvm.tran.firewall.intf.IntfDBMatcher;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolDBMatcher;

public class RedirectInternal
{
    private final boolean isEnabled;
    private final boolean isLoggingEnabled;

    private final String name;
    private final String category;
    private final String description;

    private final ProtocolDBMatcher protocol;
    private final IntfDBMatcher srcIntf;
    private final IntfDBMatcher dstIntf;
    private final IPDBMatcher srcAddress;
    private final IPDBMatcher dstAddress;
    private final PortDBMatcher srcPort;
    private final PortDBMatcher dstPort;
    
    private final boolean isDstRedirect;       /* presently unsupported */
    private final boolean isLocalRedirect;
    private final IPaddr redirectAddress;
    private final int redirectPort;
    private final int index;

    public RedirectInternal( boolean       isEnabled, boolean isLoggingEnabled,
                             String name, String category, String description,
                             ProtocolDBMatcher protocol,
                             IntfDBMatcher srcIntf,    IntfDBMatcher     dstIntf,
                             IPDBMatcher   srcAddress, IPDBMatcher       dstAddress,
                             PortDBMatcher srcPort,    PortDBMatcher     dstPort,
                             boolean isDstRedirect, IPaddr redirectAddress, int redirectPort, int index,
                             boolean isLocalRedirect )
    {
        this.isEnabled        = isEnabled;
        this.isLoggingEnabled = isLoggingEnabled;
        this.name             = name;
        this.category         = category;
        this.description      = description;
        this.protocol         = protocol;
        this.srcIntf          = srcIntf;
        this.dstIntf          = dstIntf;
        this.srcAddress       = srcAddress;
        this.dstAddress       = dstAddress;
        this.srcPort          = srcPort;
        this.dstPort          = dstPort;
        this.isDstRedirect    = isDstRedirect;
        this.redirectAddress  = redirectAddress;
        this.redirectPort     = redirectPort;
        this.index            = index;
        this.isLocalRedirect  = isLocalRedirect;
    }

    public RedirectInternal( RedirectRule rule, int index )
    {
        this( rule.isLive(), rule.getLog(), rule.getName(), rule.getCategory(), rule.getDescription(),
              rule.getProtocol(), rule.getSrcIntf(), rule.getDstIntf(),
              rule.getSrcAddress(), rule.getDstAddress(),
              rule.getSrcPort(), rule.getDstPort(),
              rule.isDstRedirect(), rule.getRedirectAddress(), rule.getRedirectPort(), index,
              rule.isLocalRedirect());
    }

    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    public boolean getIsLoggingEnabled()
    {
        return this.isLoggingEnabled;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getCategory()
    {
        return this.category;
    }

    public ProtocolDBMatcher getProtocol()
    {
        return this.protocol;
    }

    public IntfDBMatcher getSrcIntf()
    {
        return this.srcIntf;
    }

    public IntfDBMatcher getDstIntf()
    {
        return this.dstIntf;
    }

    public IPDBMatcher getSrcAddress()
    {
        return this.srcAddress;
    }
    
    public IPDBMatcher getDstAddress()
    {
        return this.dstAddress;
    }
    
    public PortDBMatcher getSrcPort()
    {
        return this.srcPort;
    }
    
    public PortDBMatcher getDstPort()
    {
        return this.dstPort;
    }

    public boolean getIsDstRedirect()
    {
        return this.isDstRedirect;
    }
    
    public IPaddr getRedirectAddress()
    {
        return this.redirectAddress;
    }

    public int getRedirectPort()
    {
        return this.redirectPort;
    }

    public int getIndex()
    {
        return this.index;
    }

    public boolean isLocalRedirect()
    {
        return this.isLocalRedirect;
    }

    /* Return a new rule that is prepopulated with the value from this list */
    public RedirectRule toRule()
    {
        RedirectRule rule = new RedirectRule( getIsEnabled(),
                                              getProtocol(), getSrcIntf(), getDstIntf(),
                                              getSrcAddress(), getDstAddress(),
                                              getSrcPort(), getDstPort(),
                                              getIsDstRedirect(), getRedirectAddress(), getRedirectPort());
        rule.setLog( getIsLoggingEnabled());
        rule.setName( getName());
        rule.setCategory( getCategory());
        rule.setDescription( getDescription());
        rule.setLocalRedirect( isLocalRedirect());
        
        return rule;
    }
}
