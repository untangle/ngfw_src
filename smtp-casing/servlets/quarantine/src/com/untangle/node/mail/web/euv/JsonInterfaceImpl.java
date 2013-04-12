/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.node.smtp.mime.EmailAddress;

import com.untangle.uvm.node.ParseException;

import com.untangle.node.smtp.quarantine.BadTokenException;
import com.untangle.node.smtp.quarantine.InboxAlreadyRemappedException;
import com.untangle.node.smtp.quarantine.InboxIndex;
import com.untangle.node.smtp.quarantine.InboxRecord;
import com.untangle.node.smtp.quarantine.InboxRecordCursor;
import com.untangle.node.smtp.quarantine.NoSuchInboxException;
import com.untangle.node.smtp.quarantine.QuarantineUserActionFailedException;
import com.untangle.node.smtp.safelist.NoSuchSafelistException;
import com.untangle.node.smtp.safelist.SafelistEndUserView;
import com.untangle.node.smtp.safelist.SafelistActionFailedException;

import com.untangle.node.smtp.quarantine.QuarantineUserView;

import static com.untangle.node.smtp.quarantine.InboxRecordComparator.SortBy;

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

    public boolean requestDigest(String account)
        throws ParseException, QuarantineUserActionFailedException
    {
        if (account == null) throw new ParseException( "Missing account" );

        //Validate at least basic format
        if (EmailAddress.parseNE(account) == null) throw new ParseException("Invalid Email Address");
        
        QuarantineUserView quarantine =
            QuarantineEnduserServlet.instance().getQuarantine();
        if(quarantine == null) {
            throw new QuarantineUserActionFailedException(Constants.SERVER_UNAVAILABLE_ERRO_VIEW);
        }

        try {
            return quarantine.requestDigestEmail(account);
        } catch(NoSuchInboxException ex) {
            return false;
        }
    }

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

    public ActionResponse releaseMessages( String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        return handleMessages( INBOX_ACTION.RELEASE, token, messages );
    }
    
    public ActionResponse purgeMessages( String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        return handleMessages( INBOX_ACTION.PURGE, token, messages );
    }

    public ActionResponse safelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException,
               QuarantineUserActionFailedException, SafelistActionFailedException
    {
        return handleSafelist( SAFELIST_ACTION.ADD, token, addresses );
    }

    /* Replace the safelist for the account associated with token. */
    public ActionResponse replaceSafelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException
    {
        return handleSafelist( SAFELIST_ACTION.REPLACE, token, addresses );
    }

    public ActionResponse deleteAddressesFromSafelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException
    {
        return handleSafelist( SAFELIST_ACTION.DELETE, token, addresses );
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

    private ActionResponse handleMessages( INBOX_ACTION action, String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine =
            QuarantineEnduserServlet.instance().getQuarantine();
                
        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if ( account == null ) return ActionResponse.EMPTY;

        InboxIndex index = quarantine.getInboxIndex( account );

        if ( index == null ) return ActionResponse.EMPTY;

        int currentSize = index.size();

        ActionResponse response = null;
        
        switch ( action ) {
        case PURGE:
            index = quarantine.purge( account, messages );
            response = new ActionResponse( index.size(), null );
            response.setPurgeCount( currentSize - index.size());
            break;
        case RELEASE:
            index = quarantine.rescue( account, messages );
            response = new ActionResponse( index.size(), null );
            response.setReleaseCount( currentSize - index.size());
            break;
        }

        return response;
    }

    private ActionResponse handleSafelist( SAFELIST_ACTION action, String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();
        
        /* This just seems wrong */
        SafelistEndUserView safelist = QuarantineEnduserServlet.instance().getSafelist();

        if (( addresses.length == 0 ) && ( action != SAFELIST_ACTION.REPLACE )) return ActionResponse.EMPTY;
                
        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if ( account == null ) return ActionResponse.EMPTY;

        InboxIndex index = quarantine.getInboxIndex( account );

        if ( index == null ) return ActionResponse.EMPTY;
        
        
        String[] sl = new String[addresses.length];
        for(int c = 0; c< addresses.length; c++) sl[c] = addresses[c].toLowerCase();

        String[] userSafelist = safelist.getSafelistContents( account );
        int currentSafelistSize = userSafelist.length;

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
        int releaseCount = 0;
        if ( mids.size() > 0 ) {
            ActionResponse temp = handleMessages( INBOX_ACTION.RELEASE, token, mids.toArray( new String[mids.size()]));
            totalRecords = temp.getTotalRecords();
            releaseCount = temp.getReleaseCount();
        }

        ActionResponse response = new ActionResponse( totalRecords, userSafelist );
        response.setReleaseCount( releaseCount );

        response.setSafelistCount( userSafelist.length - currentSafelistSize );
        return response;
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

