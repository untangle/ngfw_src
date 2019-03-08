/**
 * $Id$
 */
package com.untangle.app.smtp.web.euv;

import java.util.List;

import com.untangle.app.smtp.quarantine.BadTokenException;
import com.untangle.app.smtp.quarantine.InboxAlreadyRemappedException;
import com.untangle.app.smtp.quarantine.InboxRecord;
import com.untangle.app.smtp.quarantine.NoSuchInboxException;
import com.untangle.app.smtp.quarantine.QuarantineUserActionFailedException;
import com.untangle.app.smtp.safelist.NoSuchSafelistException;
import com.untangle.app.smtp.safelist.SafelistActionFailedException;

/**
 * Json Interface interface.
 */
public interface JsonInterface
{
    /**
     * Verify digest can be sent.
     *
     * @param  account String of account name.
     * @return                                     true if digest could be sent, otherwise false.
     * @throws QuarantineUserActionFailedException Unable to access quarantine for this account.
     */
    public boolean requestDigest( String account )
        throws QuarantineUserActionFailedException;

    /**
     * Get list of inbox records.
     *
     * @param  token                               String of token to identify the account.
     * @return                                     List of InboxRecord objects.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     */
    public List<InboxRecord> getInboxRecords( String token )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * Release the specified messages from the inbox.
     *
     * @param  token                               String of token identifying the account.
     * @param  messages                            Array of message identifiers to release.
     * @return                                     ActionResponse of releasing the messages.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     */
    public ActionResponse releaseMessages( String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * Delete all messages in the inbox.
     *
     * @param  token                               String of token identifying the account.
     * @param  messages                           Array of message identifiers to release.
     * @return                                     ActionResponse of releasing the messages.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     */
    public ActionResponse purgeMessages( String token, String messages[] )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * Add list of email addresses to account's safelist. 
     *
     * @param  token                               String of token identifying the account.
     * @param  addresses                           Array of email addresses.
     * @return                                     ActionResponse of releasing the messages.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws NoSuchSafelistException             Safelist does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     * @throws SafelistActionFailedException       Unable to add safelist.
     */
    public ActionResponse safelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException;

    /**
     * Replace the safelist for the account associated with token.
     *
     * @param  token                               String of token identifying the account.
     * @param  addresses                           Array of email addresses to replace.
     * @return                                     ActionResponse of releasing the messages.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws NoSuchSafelistException             Safelist does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     * @throws SafelistActionFailedException       Unable to add safelist.
     */
    public ActionResponse replaceSafelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException;

    /**
     * Remove addresses from mthe account safelist.
     *
     * @param  token                               String of token identifying the account.
     * @param  addresses                           Array of email addresses to remove.
     * @return                                     ActionResponse of releasing the messages.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws NoSuchSafelistException             Safelist does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     * @throws SafelistActionFailedException       Unable to add safelist.
     */
    public ActionResponse deleteAddressesFromSafelist( String token, String addresses[] )
        throws BadTokenException, NoSuchInboxException, NoSuchSafelistException, QuarantineUserActionFailedException, SafelistActionFailedException;

    /**
     * Map the account associated with token to address.
     *
     * @param  token                               String of token identifying the account.
     * @param  address                             Email address to map token.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     * @throws InboxAlreadyRemappedException       Inbox already remapped.
     */
    public void setRemap( String token, String address )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException,
               InboxAlreadyRemappedException;

    /**
     * Delete the account associated with token to address.
     *
     * @param  token                               String of token identifying the account.
     * @param  address                             Email address to remove.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     * @throws InboxAlreadyRemappedException       Inbox already remapped.
     */
    public void deleteRemap( String token, String address )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException,
               InboxAlreadyRemappedException;

    /**
     * Delete a set of remaps to the account associated with token.
     *
     * @param  token                               String of token identifying the account.
     * @param  addresses                           Email addresses to remove.
     * @return                                     String of addresses that are mapped to this address.
     * @throws BadTokenException                   Invalid token specified.
     * @throws NoSuchInboxException                Inbox does not exist.
     * @throws QuarantineUserActionFailedException Unable to perform action.
     */
    public String[] deleteRemaps( String token, String[] addresses )
        throws BadTokenException, NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * Return the system timezone offset.
     * 
     * @return Integer value of the offset in miniseconds.
     */
    public Integer getTimeZoneOffset();
}
