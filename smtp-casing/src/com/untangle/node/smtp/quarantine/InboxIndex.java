/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine;
import java.io.Serializable;
import java.util.Iterator;

/**
 *
 */
public interface InboxIndex
    extends Iterable<InboxRecord>, Serializable
{
    /**
     * Iterate over the contents of this
     * InboxIndex (can be used with the nifty-new
     * 1.5 "foreach" loops).
     */
    public Iterator<InboxRecord> iterator();

    /**
     * Get the email address of the "owner" of this inbox
     */
    public String getOwnerAddress();

    /**
     * Get the time that this inbox was last accessed.  This
     * includes additions, as well as any end-user maintenence.
     */
    public long getLastAccessTimestamp();

    /**
     * Get the number of mails within the index (inbox)
     * - HashMap size() method
     */
    public int size();

    public int inboxCount();
    public long inboxSize();

    /**
     * Returns null if not found
     */
    public InboxRecord getRecord(String id);

    public InboxRecord[] allRecords();
}
