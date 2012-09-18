/*
 * $Id: HostTable.java,v 1.00 2012/08/29 10:41:35 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Hashtable;
import java.net.InetAddress;

public interface HostTable
{
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

    public final String KEY_HOSTNAME = "platform-hostname"; /* String - stores the hostname associated with this host */
    public final String KEY_USERNAME = "platform-username"; /* String - stores the username associated with this host (can come from several places) */
    public final String KEY_USERNAME_SOURCE = "platform-username-source"; /* String - stores the source of the global username above */
    public final String KEY_PENALTY_BOXED = "platform-penaltyboxed"; /* Boolean  - stores whether or not this host is "penalty boxed" */
    public final String KEY_PENALTY_BOX_EXIT_TIME = "platform-penaltybox-exit-time"; /* Long  - stores the exit time for the penalty box if this host is penalty boxed */
    public final String KEY_PENALTY_BOX_ENTRY_TIME = "platform-penaltybox-entry-time"; /* Long  - stores the entry time for the penalty box if this host is penalty boxed */

    public final String KEY_ADCONNECTOR_USERNAME = "adconnector-username"; /* String - stores the username associated with this host according to adconnector/adpb */

    public final String KEY_CAPTURE_USERNAME = "capture-username"; /* String  - stores the username associated with this host according to capture */

    public final String KEY_PENALTY_BOX_PRIORITY = "bandwidth-penaltybox-priority"; /* Integer  - stores the bandwidth control priority for this penalty boxed host if bandwidth control penalty boxed this host */

    public final String[] ALL_ATTACHMENTS = new String[] {
        HostTable.KEY_HOSTNAME,
        HostTable.KEY_USERNAME,
        HostTable.KEY_USERNAME_SOURCE,
        HostTable.KEY_PENALTY_BOXED,
        HostTable.KEY_PENALTY_BOX_EXIT_TIME,
        HostTable.KEY_PENALTY_BOX_ENTRY_TIME,
        HostTable.KEY_ADCONNECTOR_USERNAME,
        HostTable.KEY_CAPTURE_USERNAME,
        HostTable.KEY_PENALTY_BOX_PRIORITY
    };
    
    public void setAttachment(InetAddress addr, String key, Object ob);

    public Object getAttachment(InetAddress addr, String key);

    public String[] getPossibleAttachments();

    public LinkedList<HostTableEntry> getHosts();
    
}

