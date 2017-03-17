/**
 * $Id$
 */
package com.untangle.app.smtp.web.euv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.untangle.app.smtp.mime.MIMEUtil;
import com.untangle.app.smtp.quarantine.BadTokenException;
import com.untangle.app.smtp.quarantine.InboxAlreadyRemappedException;
import com.untangle.app.smtp.quarantine.InboxIndex;
import com.untangle.app.smtp.quarantine.InboxRecord;
import com.untangle.app.smtp.quarantine.NoSuchInboxException;
import com.untangle.app.smtp.quarantine.QuarantineUserActionFailedException;
import com.untangle.app.smtp.quarantine.QuarantineUserView;
import com.untangle.app.smtp.safelist.NoSuchSafelistException;
import com.untangle.app.smtp.safelist.SafelistActionFailedException;
import com.untangle.app.smtp.safelist.SafelistManipulation;

public class JsonInterfaceImpl implements JsonInterface
{
    public static final int DEFAULT_LIMIT = 20;

    private static enum INBOX_ACTION
    {
        PURGE, RELEASE
    };

    private static enum SAFELIST_ACTION
    {
        REPLACE, ADD, DELETE
    };

    private static final JsonInterfaceImpl INSTANCE = new JsonInterfaceImpl();

    public boolean requestDigest(String account) throws QuarantineUserActionFailedException
    {
        if (account == null)
            throw new RuntimeException("Missing account");

        // Validate at least basic format
        try {
            if (MIMEUtil.parseEmailAddress(account) == null)
                throw new RuntimeException("Invalid Email Address");
        } catch (Exception e) {
            throw new RuntimeException("Invalid Email Address");
        }

        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();
        if (quarantine == null) {
            throw new QuarantineUserActionFailedException();
        }

        try {
            return quarantine.requestDigestEmail(account);
        } catch (NoSuchInboxException ex) {
            return false;
        }
    }

    public List<InboxRecord> getInboxRecords(String token)
            throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();

        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if (account == null)
            return new ArrayList<InboxRecord>();

        List<InboxRecord> inboxRecords = quarantine.getInboxRecords(account);

        if (inboxRecords == null)
            return new ArrayList<InboxRecord>();
        return inboxRecords;
    }

    public ActionResponse releaseMessages(String token, String messages[]) throws BadTokenException,
            NoSuchInboxException, QuarantineUserActionFailedException
    {
        return handleMessages(INBOX_ACTION.RELEASE, token, messages);
    }

    public ActionResponse purgeMessages(String token, String messages[]) throws BadTokenException,
            NoSuchInboxException, QuarantineUserActionFailedException
    {
        return handleMessages(INBOX_ACTION.PURGE, token, messages);
    }

    public ActionResponse safelist(String token, String addresses[]) throws BadTokenException, NoSuchInboxException,
            NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException
    {
        return handleSafelist(SAFELIST_ACTION.ADD, token, addresses);
    }

    /* Replace the safelist for the account associated with token. */
    public ActionResponse replaceSafelist(String token, String addresses[]) throws BadTokenException,
            NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException,
            SafelistActionFailedException
    {
        return handleSafelist(SAFELIST_ACTION.REPLACE, token, addresses);
    }

    public ActionResponse deleteAddressesFromSafelist(String token, String addresses[]) throws BadTokenException,
            NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException,
            SafelistActionFailedException
    {
        return handleSafelist(SAFELIST_ACTION.DELETE, token, addresses);
    }

    /* Map the account associated with token to address. */
    public void setRemap(String token, String address) throws BadTokenException, NoSuchInboxException,
            QuarantineUserActionFailedException, InboxAlreadyRemappedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();

        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if (account == null)
            return;

        quarantine.remapSelfService(account, address);
    }

    /* Delete the account associated with token to address. */
    public void deleteRemap(String token, String address) throws BadTokenException, NoSuchInboxException,
            QuarantineUserActionFailedException, InboxAlreadyRemappedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();

        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if (account == null)
            return;

        quarantine.unmapSelfService(address, account);
    }

    /* Delete a set of remaps to the account associated with token. */
    public String[] deleteRemaps(String token, String[] addresses) throws BadTokenException, NoSuchInboxException,
            QuarantineUserActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();

        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if (account == null)
            return new String[0];

        for (String address : addresses)
            quarantine.unmapSelfService(account, address.toLowerCase());

        return quarantine.getMappedFrom(account);
    }

    public static JsonInterfaceImpl getInstance()
    {
        return INSTANCE;
    }

    private ActionResponse handleMessages(INBOX_ACTION action, String token, String messages[])
            throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();

        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if (account == null)
            return ActionResponse.EMPTY;

        List<InboxRecord> inboxRecords = quarantine.getInboxRecords(account);

        if (inboxRecords == null)
            return ActionResponse.EMPTY;

        int currentSize = inboxRecords.size();

        ActionResponse response = null;
        InboxIndex index = null;
        switch (action) {
            case PURGE:
                index = quarantine.purge(account, messages);
                response = new ActionResponse(index.size(), null);
                response.setPurgeCount(currentSize - index.size());
                break;
            case RELEASE:
                index = quarantine.rescue(account, messages);
                response = new ActionResponse(index.size(), null);
                response.setReleaseCount(currentSize - index.size());
                break;
        }

        return response;
    }

    private ActionResponse handleSafelist(SAFELIST_ACTION action, String token, String addresses[])
            throws BadTokenException, NoSuchInboxException, NoSuchSafelistException,
            QuarantineUserActionFailedException, SafelistActionFailedException
    {
        /* This just seems wrong */
        QuarantineUserView quarantine = QuarantineEnduserServlet.instance().getQuarantine();

        /* This just seems wrong */
        SafelistManipulation safelist = QuarantineEnduserServlet.instance().getSafelist();

        if ((addresses.length == 0) && (action != SAFELIST_ACTION.REPLACE))
            return ActionResponse.EMPTY;

        /* First grab the account */
        String account = quarantine.getAccountFromToken(token);

        if (account == null)
            return ActionResponse.EMPTY;

        List<InboxRecord> inboxRecords = quarantine.getInboxRecords(account);

        if (inboxRecords == null)
            return ActionResponse.EMPTY;

        List<String> sl = new ArrayList<String>();
        for (int c = 0; c < addresses.length; c++){
            if (addresses[c] != null)
                sl.add(addresses[c].toLowerCase());
        }
        
        if ((sl.size() == 0) && (action != SAFELIST_ACTION.REPLACE))
            return ActionResponse.EMPTY;

        String[] userSafelist = safelist.getSafelistContents(account);
        int currentSafelistSize = userSafelist.length;

        /* Add each of the entries. */
        switch (action) {
            case REPLACE:
                userSafelist = safelist.replaceSafelist(account, sl.toArray(new String[0]));
                break;
            case ADD:
                for (String addr : sl)
                    userSafelist = safelist.addToSafelist(account, addr);
                break;

            case DELETE:
                for (String addr : sl)
                    userSafelist = safelist.removeFromSafelist(account, addr);
                break;
        }

        Set<String> mids = new HashSet<String>();

        /* Now build a list of message ids to release */
        if (action != SAFELIST_ACTION.DELETE) {
            for (InboxRecord record : inboxRecords) {
                for (String addr : addresses) {
                    if (addr == null)
                        continue;
                    if ( record.getMailSummary() == null )
                        continue;
                    if ( record.getMailSummary().getSender() == null )
                        continue;
                    if ( record.getMailSummary().getSender().equals( addr ) ) {
                        mids.add(record.getMailID());
                        break;
                    }
                }
            }
        }

        /* Now release the messages */
        int totalRecords = inboxRecords.size();
        int releaseCount = 0;
        if (mids.size() > 0) {
            ActionResponse temp = handleMessages(INBOX_ACTION.RELEASE, token, mids.toArray(new String[mids.size()]));
            totalRecords = temp.getTotalRecords();
            releaseCount = temp.getReleaseCount();
        }

        ActionResponse response = new ActionResponse(totalRecords, userSafelist);
        response.setReleaseCount(releaseCount);

        response.setSafelistCount(userSafelist.length - currentSafelistSize);
        return response;
    }
}
