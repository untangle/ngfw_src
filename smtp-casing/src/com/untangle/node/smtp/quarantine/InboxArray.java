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

package com.untangle.node.smtp.quarantine;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A class for accessing a paginated array of data from the server.
 */
@SuppressWarnings("serial")
public final class InboxArray implements Serializable
{
    private final Inbox[] inboxes;

    /* This is the total number of records in the inbox (not the size of inboxes) */
    private final int totalRecords;

    public InboxArray( Inbox[] inboxes, int totalRecords )
    {
        this.inboxes = inboxes;
        this.totalRecords = totalRecords;
    }

    public Inbox[] getInboxes()
    {
        return this.inboxes;
    }

    public int getTotalRecords()
    {
        return this.totalRecords;
    }

    public static InboxArray getInboxArray( Inbox[] allInboxes, InboxComparator.SortBy sortBy,
                                            int startingAt, int limit, boolean ascending )
    {
        Arrays.sort( allInboxes, InboxComparator.getComparator( sortBy, ascending ));

        if ( startingAt >= allInboxes.length ) startingAt = allInboxes.length - 1 - limit;
        
        if ( startingAt < 0 ) startingAt = 0;

        if (( startingAt + limit ) > allInboxes.length ) limit = allInboxes.length - startingAt;

        Inbox[] inboxes = new Inbox[limit];
        System.arraycopy( allInboxes, startingAt, inboxes, 0, limit );

        return new InboxArray( inboxes, allInboxes.length );
    }
}
