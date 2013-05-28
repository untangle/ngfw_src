/**
 * $Id: InstallAndInstantiateComplete.java,v 1.00 2013/05/27 15:29:16 dmorris Exp $
 */
package com.untangle.uvm.apt;

import com.untangle.uvm.message.Message;

/**
 * Signals that install and instantiate is complete.
 */
@SuppressWarnings("serial")
public class InstallAndInstantiateComplete extends Message
{

    private final PackageDesc requestingPackage;

    public InstallAndInstantiateComplete(PackageDesc requestingPackage)
    {
        this.requestingPackage = requestingPackage;
    }

    // accessors --------------------------------------------------------------

    public PackageDesc getRequestingPackage()
    {
        return requestingPackage;
    }
}
