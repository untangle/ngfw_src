/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
public class MIMESourceRecord {

    public final MIMESource source;
    public final int start;
    public final int len;
    private boolean m_shared;

    public MIMESourceRecord(MIMESource source,
                            int start,
                            int len,
                            boolean shared) {

        this.source = source;
        this.start = start;
        this.len = len;
        m_shared = shared;

    }

    /**
     * Is the underlying MIMESource shared by the owner
     * of this record and other objects.
     */
    public boolean isShared() {
        return m_shared;
    }
    public void setShared(boolean shared) {
        m_shared = shared;
    }

}
