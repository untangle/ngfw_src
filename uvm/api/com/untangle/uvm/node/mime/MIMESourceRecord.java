/**
 * $Id$
 */
package com.untangle.node.mime;

/**
 * Class to encapsulate a given MIME atrifact's
 * offset/length within a MIMESource.
 * <br>
 * MIMESourceRecords are unique to each consumer of
 * a MIMESource, and are never intended to be shared
 * references.  Instead, MIMESources may be shared but
 * instances of this class offer the {@link #isShared shared}
 * property which can be used to detect if the holder
 * of the record can close the MIMESource.
 * <br>
 * This of course assumes a parent/child relationship, where
 * a parent may have children whose data is part of its MIMESource.
 * In that situation, the children's MIMESourceRecords
 * have a shared property of "true" where the parent's
 * record indicates "false".
 */
public class MIMESourceRecord
{
    public final MIMESource source;
    public final int start;
    public final int len;
    private boolean m_shared;

    public MIMESourceRecord( MIMESource source, int start, int len, boolean shared )
    {
        this.source = source;
        this.start = start;
        this.len = len;
        m_shared = shared;
    }

    /**
     * Is the underlying MIMESource shared by the owner
     * of this record and other objects.
     */
    public boolean isShared()
    {
        return m_shared;
    }

    public void setShared(boolean shared)
    {
        m_shared = shared;
    }

}
