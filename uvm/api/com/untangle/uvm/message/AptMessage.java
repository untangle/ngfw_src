/*
 * $Id: AptInstallMessage.java,v 1.00 2011/09/21 14:32:43 dmorris Exp $
 */
package com.untangle.uvm.message;

import com.untangle.uvm.apt.PackageDesc;

/**
 * Sends some apt action message to the UI
 */
@SuppressWarnings("serial")
public class AptMessage extends Message
{
    private final String action; /* "install" or "unpack" or "alldone" */
    private final Integer count;
    private final Integer totalCount;
    private final PackageDesc requestingPackage;
    
    public AptMessage(String action, PackageDesc requestingPackage, int count, int totalCount)
    {
        this.action = action;
        this.requestingPackage = requestingPackage;
        this.count = count;
        this.totalCount = totalCount;
    }

    public String getAction()
    {
        return action;
    }

    public PackageDesc getRequestingPackage()
    {
        return requestingPackage;
    }

    public Integer getCount()
    {
        return count;
    }

    public Integer getTotalCount()
    {
        return totalCount;
    }
}
