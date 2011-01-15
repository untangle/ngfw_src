package com.untangle.uvm.engine;

import org.apache.log4j.Logger;

import com.untangle.uvm.ArgonException;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.node.RemoteIntfManager;
import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;

/**
 * Passthru class for access to the api function inside of the
 * interface manager
 */
public class RemoteIntfManagerImpl implements RemoteIntfManager
{
    private final LocalIntfManager localIntfManager;

    private final Logger logger = Logger.getLogger( this.getClass());

    RemoteIntfManagerImpl( LocalIntfManager lim )
    {
        this.localIntfManager = lim;
    }

    public void loadInterfaceConfig()
    {
        try {
            localIntfManager.loadInterfaceConfig();
        } catch ( ArgonException e ) {
            logger.warn( "Unable to loadInterafceConfig", e );
        }
    }

    public IntfDBMatcher[] getIntfMatcherEnumeration()
    {
        return this.localIntfManager.getIntfMatcherEnumeration();
    }
}
