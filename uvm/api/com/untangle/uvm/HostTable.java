/*
 * $Id: HostTable.java,v 1.00 2012/08/29 10:41:35 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Hashtable;
import java.net.InetAddress;

import com.untangle.uvm.node.EventLogQuery;

/**
 * The Host Table is responsible for storing known information about hosts.
 * Many different components use the host table to share known information about a given host (for example, its hostname).
 * Each host has a table of information known about it and these "attachments" can be read/written using keys
 *
 * The host table also contains penalty box methods which maintain all the host table attachments related to penalty box logic
 */
public interface HostTable
{
    /**
     * This is a host table entry
     * It stores the address, and a table of all the known information about this host (attachments)
     */
    public class HostTableEntry
    {
        public InetAddress addr;
        public Hashtable<String, Object> attachments;
        public long creationTime;
        public long lastAccessTime;

        public InetAddress getAddr() { return this.addr; }
        public Hashtable<String, Object> getAttachments() { return this.attachments; }
        public long getCreationTime() { return this.creationTime; }
        public long getLastAccessTime() { return this.lastAccessTime; }
        
    };

    /**
     * This is a list of all the various keys stored in a host's attachment map
     */
    public final String KEY_HOSTNAME = "hostname"; /* String - stores the hostname associated with this host */
    public final String KEY_USERNAME = "username"; /* String - stores the username associated with this host (can come from several places) */
    public final String KEY_USERNAME_SOURCE = "username-source"; /* String - stores the source of the global username above */
    public final String KEY_PENALTY_BOXED = "penaltybox"; /* Boolean  - stores whether or not this host is "penalty boxed" */
    public final String KEY_PENALTY_BOX_EXIT_TIME = "penaltybox-exit-time"; /* Long  - stores the exit time for the penalty box if this host is penalty boxed */
    public final String KEY_PENALTY_BOX_ENTRY_TIME = "penaltybox-entry-time"; /* Long  - stores the entry time for the penalty box if this host is penalty boxed */
    public final String KEY_ADCONNECTOR_USERNAME = "adconnector-username"; /* String - stores the username associated with this host according to adconnector/adpb */
    public final String KEY_CAPTURE_USERNAME = "capture-username"; /* String  - stores the username associated with this host according to capture */
    public final String KEY_HTTP_AGENT_STRING = "http-user-agent"; /* String  - stores the HTTP agent string that the client presented in the header */
    public final String KEY_HTTP_AGENT_STRING_OS = "http-user-agent-os"; /* String  - stores OS informanio from the HTTP agent string that the client presented in the header */
    public final String KEY_PENALTY_BOX_PRIORITY = "penaltybox-priority"; /* Integer  - stores the bandwidth control priority for this penalty boxed host if bandwidth control penalty boxed this host */

    /**
     * A list of all attachment keys
     */
    public final String[] ALL_ATTACHMENTS = new String[] {
        HostTable.KEY_HOSTNAME,
        HostTable.KEY_USERNAME,
        HostTable.KEY_USERNAME_SOURCE,
        HostTable.KEY_PENALTY_BOXED,
        HostTable.KEY_PENALTY_BOX_EXIT_TIME,
        HostTable.KEY_PENALTY_BOX_ENTRY_TIME,
        HostTable.KEY_ADCONNECTOR_USERNAME,
        HostTable.KEY_CAPTURE_USERNAME,
        HostTable.KEY_HTTP_AGENT_STRING,
        HostTable.KEY_HTTP_AGENT_STRING_OS,
        HostTable.KEY_PENALTY_BOX_PRIORITY
    };
    
    /**
     * Set the attachment on a host's table for the specified key to the specified object
     * Will create a host table entry if needed
     */
    public void setAttachment( InetAddress addr, String key, Object ob );

    /**
     * Get the attachment on a host's table for the specified key
     * Returns null if there is no host table entry for this host
     * Returns null if there is no attachment for this key for this host
     */
    public Object getAttachment( InetAddress addr, String key );

    /**
     * Returns ALL_ATTACHMENTS
     */
    public String[] getPossibleAttachments();

    /**
     * Returns a duplicated list of all current hosts
     */
    public LinkedList<HostTableEntry> getHosts();

    /**
     * Return the event log query for host table updates
     */
    public EventLogQuery[] getHostTableEventQueries();

    /**
     * Add a host to the penalty box for the specified amount of time at the specified priority
     * This sets all the appropriate attachments and calls the listeners
     */
    public void addHostToPenaltyBox( InetAddress address, int priority, int time_sec );

    /**
     * Release a host from the penalty box
     * This sets all the appropriate attachments and calls the listeners
     */
    public void releaseHostFromPenaltyBox( InetAddress address );
    
    /**
     * Checks if a host is in the penalty box
     */
    public boolean hostInPenaltyBox( InetAddress address );

    /**
     * Returns a current list of all hosts in the penalty box
     * This is used by the UI to display the list
     */
    public LinkedList<HostTable.HostTableEntry> getPenaltyBoxedHosts();

    /**
     * Register a penalty box listener
     */
    public void registerListener( PenaltyBoxListener listener );

    /**
     * Unregister a penalty box listener
     */
    public void unregisterListener( PenaltyBoxListener listener );

    /**
     * Return the event log query for penalty box events
     */
    public EventLogQuery[] getPenaltyBoxEventQueries();

    /**
     * A penalty box listener is a hook called when hosts enter or exit the penalty box
     */
    public interface PenaltyBoxListener
    {
        public void enteringPenaltyBox( InetAddress addr );
        public void exitingPenaltyBox( InetAddress addr );
    }
    
}

