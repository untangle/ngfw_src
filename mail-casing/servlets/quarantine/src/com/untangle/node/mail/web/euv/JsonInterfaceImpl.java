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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.node.mail.papi.quarantine.BadTokenException;
import com.untangle.node.mail.papi.quarantine.InboxAlreadyRemappedException;
import com.untangle.node.mail.papi.quarantine.InboxIndex;
import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.mail.papi.quarantine.InboxRecordComparator;
import com.untangle.node.mail.papi.quarantine.InboxRecordCursor;
import com.untangle.node.mail.papi.quarantine.NoSuchInboxException;
import com.untangle.node.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.untangle.node.mail.papi.safelist.NoSuchSafelistException;
import com.untangle.node.mail.papi.safelist.SafelistEndUserView;
import com.untangle.node.mail.papi.safelist.SafelistActionFailedException;

import com.untangle.node.mail.papi.quarantine.QuarantineUserView;

import static com.untangle.node.mail.papi.quarantine.InboxRecordComparator.SortBy;

public class JsonInterfaceImpl implements JsonInterface
{
    public static final int DEFAULT_LIMIT = 20;

    private static final Map<String, SortBy> NAME_TO_SORT_BY;
    private static final SortBy DEFAULT_SORT_COLUMN = SortBy.INTERN_DATE;

    private static enum INBOX_ACTION
    {
        PURGE,
        RELEASE
    };

    private static enum SAFELIST_ACTION
    {
        REPLACE,
        ADD,
        DELETE
    };

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

        if ( account == null ) return new ArrayList<JsonInboxRecord>();
        
        InboxIndex index = quarantine.getInboxIndex(account);

        if ( index == null ) return new ArrayList<JsonInboxRecord>();

        if ( start < 0 ) start = 0;
        if ( limit <= 0 ) limit = DEFAULT_LIMIT;

        SortBy sortBy = NAME_TO_SORT_BY.get( sortColumn );
        if ( sortBy == null ) sortBy = DEFAULT_SORT_COLUMN;

        InboxRecordCursor cursor = 
            InboxRecordCursor.get( index.getAllRecords(), sortBy, isAscending, start, limit );

        List<JsonInboxRecord> records = new ArrayList<JsonInboxRecord>( cursor.size());

        for ( InboxRecord record : cursor ) records.add( new JsonInboxRecord( record ));
        
        return records;
    }

    public int releaseMessages( String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        return handleMessages( INBOX_ACTION.RELEASE, token, messages );
    }
    
    public int purgeMessages( String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        return handleMessages( INBOX_ACTION.PURGE, token, messages );
    }

    public SafelistReturnCode safelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException,
               QuarantineUserActionFailedException, SafelistActionFailedException
    {
        return handleSafelist( SAFELIST_ACTION.ADD, token, addresses );
    }

    /* Replace the safelist for the account associated with token. */
    public SafelistReturnCode replaceSafelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException
    {
        return handleSafelist( SAFELIST_ACTION.REPLACE, token, addresses );
    }

    /* Map the account associated with token to address. */
    public void setRemap( String token, String address )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException,
               InboxAlreadyRemappedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine =
            QuarantineEnduserServlet.instance().getQuarantine();
        
        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);
        
        if ( account == null ) return;
        
        quarantine.remapSelfService( account, address );
    }

    /* Delete a set of remaps to the account associated with token. */
    public String[] deleteRemaps( String token, String[] addresses )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine =
            QuarantineEnduserServlet.instance().getQuarantine();
        
        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);
        
        if ( account == null ) return new String[0];
        
        for ( String address : addresses ) quarantine.unmapSelfService( account, address.toLowerCase());

        return quarantine.getMappedFrom(account);
    }


    public static JsonInterfaceImpl getInstance()
    {
        return INSTANCE;
    }

    private int handleMessages( INBOX_ACTION action, String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine =
            QuarantineEnduserServlet.instance().getQuarantine();
                
        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if ( account == null ) return 0;

        InboxIndex index = quarantine.getInboxIndex( account );

        if ( index == null ) return 0;
        
        switch ( action ) {
        case PURGE:
            index = quarantine.purge( account, messages );
            break;
        case RELEASE:
            index = quarantine.rescue( account, messages );
            break;
        }

        return index.size();
    }

    private SafelistReturnCode handleSafelist( SAFELIST_ACTION action, String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();
        
        /* This just seems wrong */
        SafelistEndUserView safelist = QuarantineEnduserServlet.instance().getSafelist();

        if (( addresses.length == 0 ) && ( action != SAFELIST_ACTION.REPLACE )) return SafelistReturnCode.EMPTY;
                
        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if ( account == null ) return SafelistReturnCode.EMPTY;

        InboxIndex index = quarantine.getInboxIndex( account );

        if ( index == null ) return SafelistReturnCode.EMPTY;

        String[] sl = new String[addresses.length];
        for(int c = 0; c< addresses.length; c++) sl[c] = addresses[c].toLowerCase();

        String[] userSafelist = safelist.getSafelistContents( account );
        int currentSize = userSafelist.length;

        /* Add each of the entries. */
        switch ( action ) {
        case REPLACE:
            userSafelist = safelist.replaceSafelist( account, sl );
            break;
        case ADD:
            for(String addr : sl) userSafelist = safelist.addToSafelist( account, addr );
            break;

        case DELETE:
            for(String addr : sl) userSafelist = safelist.removeFromSafelist( account, addr );
            break;
        }

        Set<String> mids = new HashSet<String>();

        /* Now build a list of message ids to release */
        if ( action != SAFELIST_ACTION.DELETE ) {
            for ( InboxRecord record : index ) {
                for ( String addr : addresses ) {
                    if ( record.getMailSummary().getSender().equalsIgnoreCase( addr )) {
                        mids.add( record.getMailID());
                        break;
                    }
                }
            }
        }
        
        /* Now release the messages */
        int totalRecords = index.size();
        if ( mids.size() > 0 ) {
            totalRecords = handleMessages( INBOX_ACTION.RELEASE, token, mids.toArray( new String[mids.size()]));
        }

        return new SafelistReturnCode( userSafelist.length - currentSize, totalRecords, userSafelist );
    }

    static {
        Map <String,SortBy> nameMap = new HashMap<String,SortBy>();

        nameMap.put( "sender", SortBy.SENDER );
        nameMap.put( "attachmentCount", SortBy.ATTACHMENT_COUNT );
        nameMap.put( "quarantineDetail", SortBy.DETAIL );
        nameMap.put( "truncatedSubject", SortBy.SUBJECT );
        nameMap.put( "subject", SortBy.SUBJECT );
        nameMap.put( "quarantinedDate", SortBy.INTERN_DATE );
        nameMap.put( "size", SortBy.SIZE );

        NAME_TO_SORT_BY = Collections.unmodifiableMap( nameMap );
    }
}

