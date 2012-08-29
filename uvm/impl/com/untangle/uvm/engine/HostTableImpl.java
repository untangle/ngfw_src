/**
 * $Id: HostTableImpl.java,v 1.00 2012/08/29 10:12:07 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.util.Hashtable;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.HostTable;

/**
 * HostTable stores a global table of all "local" IPs that have recently been seen.
 * This table is useful for storing information know about the various hosts.
 *
 * Different applications can add known information about various hosts by attaching objects with keys
 * Other applications can check what is known about various hosts by looking up objected stored for the various keys
 */
public class HostTableImpl implements HostTable
{
    private final Logger logger = Logger.getLogger(getClass());

    private Hashtable<InetAddress, HostTableEntry> hostTable;

    protected HostTableImpl()
    {
        this.hostTable = new Hashtable<InetAddress, HostTableEntry>();
    }
    
    public void setAttachment(InetAddress addr, String key, Object ob)
    {
        if ( addr == null || key == null ) {
            logger.warn( "Invalid arguments: setAttachment( " + addr + " , " + key + " , " + ob + " )");
            return;
        }

        logger.info("setAttachment( " + addr + " , " + key + " , " + ob + " )");

        HostTableEntry entry = getHostTableEntry( addr, true );

        if (ob != null)
            entry.attachments.put( key, ob );
        else
            entry.attachments.remove( key ); /* if null, remove object */
        
        return;
    }

    public Object getAttachment(InetAddress addr, String key)
    {
        if ( addr == null || key == null ) {
            logger.warn( "Invalid arguments: getAttachment( " + addr + " , " + key + " )");
            return null;
        }
            
        logger.debug("getAttachment( " + addr + " , " + key + " )");

        /**
         * Special treatment for USERNAME
         */
        if (key.equals(HostTable.KEY_USERNAME)) {
            String capture_username = (String) getAttachment( addr, HostTable.KEY_CAPTURE_USERNAME );
            if (capture_username != null)
                return capture_username;
            String adconnector_username = (String) getAttachment( addr, HostTable.KEY_ADCONNECTOR_USERNAME );
            if (adconnector_username != null)
                return adconnector_username;
        }
        /**
         * Special treatment for USERNAME_SOURCE
         */
        if (key.equals(HostTable.KEY_USERNAME_SOURCE)) {
            String capture_username = (String) getAttachment( addr, HostTable.KEY_CAPTURE_USERNAME );
            if (capture_username != null)
                return "capture";
            String adconnector_username = (String) getAttachment( addr, HostTable.KEY_ADCONNECTOR_USERNAME );
            if (adconnector_username != null)
                return "adconnector";
        }

        /**
         * just return whatever is found in the table
         */
        HostTableEntry entry = getHostTableEntry( addr, false );
        if (entry == null)
            return null;
        else
            return entry.attachments.get( key );
    }

    public String[] getPossibleAttachments()
    {
        return HostTable.ALL_ATTACHMENTS;
    }

    public LinkedList<HostTableEntry> getHosts()
    {
        LinkedList<HostTableEntry> hosts = new LinkedList<HostTableEntry>(hostTable.values());

        for (HostTableEntry entry: hosts) {
            /**
             * create a copy of the hash table so the original can not be modified
             * Iterate through all keys, this handles non-persistent keys like USERNAME
             */
            entry.attachments = new Hashtable<String, Object>(entry.attachments); 
            for ( String key : HostTable.ALL_ATTACHMENTS) {
                Object value = getAttachment( entry.addr, key );
                if (value != null) 
                    entry.attachments.put( key, value );
            }
        }

        return hosts;
    }
    
    private HostTableEntry getHostTableEntry( InetAddress addr, boolean createIfNecessary )
    {
        HostTableEntry entry = hostTable.get( addr );

        if ( entry == null && createIfNecessary ) {
            entry = createNewHostTableEntry( addr );
            hostTable.put( addr, entry );
        }

        return entry;
    }

    private HostTableEntry createNewHostTableEntry( InetAddress addr )
    {
        HostTableEntry entry = new HostTableEntry();

        entry.addr = addr;
        entry.attachments = new Hashtable<String, Object>();
        entry.creationTime = System.currentTimeMillis();
        entry.lastAccessTime = entry.creationTime;

        return entry;
    }

    
    
}
