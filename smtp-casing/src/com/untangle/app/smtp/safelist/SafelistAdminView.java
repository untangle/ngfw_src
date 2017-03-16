/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

import java.util.List;

/**
 * Interface for Admins to browse/manipulate the Quarantine.
 */
public interface SafelistAdminView extends SafelistManipulation
{
    /**
     * List the email addresses of all users who have a safelist
     * 
     * @return the list of all safelist owners.
     */
    public List<String> listSafelists() throws SafelistActionFailedException;

    /**
     * Delete the safelist for the given email address. Note that if there is no such account, this error is ignored.
     * 
     * @param safelistOwnerAddress
     *            the email address
     */
    public void deleteSafelist(String safelistOwnerAddress) throws SafelistActionFailedException;

    public void deleteSafelists(String[] safelistOwnerAddresses) throws SafelistActionFailedException;

    /**
     * To avoid any anoying concurrency issues, callers are permitted to attempt to create an existing safelist. In
     * other words, the implementation must perform any synchronization to prevent ill effects of duplicate list
     * creation.
     */
    public void createSafelist(String newListOwnerAddress) throws SafelistActionFailedException;

    /**
     * Test if the given Safelist exists.
     * 
     * @param safelistOwnerAddress
     * 
     * @return true if address exists
     */
    public boolean safelistExists(String safelistOwnerAddress) throws SafelistActionFailedException;

    /**
     * List the email addresses and counts of all users who have a safelist
     * 
     * @return the list of all safelist owners counts.
     */
    public List<SafelistCount> getUserSafelistCounts() throws NoSuchSafelistException, SafelistActionFailedException;

}
