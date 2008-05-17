/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.web.euv;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.untangle.node.mail.papi.quarantine.BadTokenException;
import com.untangle.node.mail.papi.quarantine.InboxIndex;
import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.mail.papi.quarantine.InboxRecordComparator;
import com.untangle.node.mail.papi.quarantine.InboxRecordCursor;
import com.untangle.node.mail.papi.quarantine.NoSuchInboxException;
import com.untangle.node.mail.papi.quarantine.QuarantineUserActionFailedException;

import com.untangle.node.mail.papi.quarantine.QuarantineUserView;

public class JsonInterfaceImpl implements JsonInterface
{
    public static final int DEFAULT_LIMIT = 20;

    private static final JsonInterfaceImpl INSTANCE = new JsonInterfaceImpl();

    public List<JsonInboxRecord> getInboxRecords( String token, int start, int limit, 
                                                  String sortColumn, boolean isAscending )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine =
            QuarantineEnduserServlet.instance().getQuarantine();
                
        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if ( start < 0 ) start = 0;
        if ( limit <= 0 ) limit = DEFAULT_LIMIT;

        if ( account == null ) return new ArrayList<JsonInboxRecord>();
        
        InboxIndex index = quarantine.getInboxIndex(account);

        if ( index == null ) return new ArrayList<JsonInboxRecord>();

        InboxRecordComparator.SortBy sortBy = InboxRecordComparator.getSortBy( sortColumn );
        if ( sortBy == null ) sortBy = InboxRecordComparator.SortBy.INTERN_DATE;

        InboxRecordCursor cursor = 
            InboxRecordCursor.get( index.getAllRecords(), sortBy, isAscending, start, limit );

        List<JsonInboxRecord> records = new ArrayList<JsonInboxRecord>( cursor.size());

        for ( InboxRecord record : cursor ) records.add( new JsonInboxRecord( record ));
        
        return records;
    }

    public static JsonInterfaceImpl getInstance()
    {
        return INSTANCE;
    }
}

