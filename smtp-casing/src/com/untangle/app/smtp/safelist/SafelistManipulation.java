/**
 * $Id$
 */
package com.untangle.app.smtp.safelist;

/**
 * Base-interface for Admin and User views into the Safelist Manager
 */
public interface SafelistManipulation
{
    /**
     * Add an entry to a safelist <br>
     * <br>
     * Requesting the addition of a duplicate address is not an error and will be silently ignored.
     * 
     * @param safelistOwnerAddress
     *            the logical address owner
     * @param toAdd
     *            the address to add
     * 
     * @return the contents of the safelist <i>after</i> the operation has been performed
     * 
     * @exception NoSuchSafelistException
     *                Note that this should not be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns
     *                true for this address
     * @exception SafelistActionFailedException
     *                general back-end problem (you're hosed)
     */
    public String[] addToSafelist(String safelistOwnerAddress, String toAdd) throws NoSuchSafelistException,
            SafelistActionFailedException;

    /**
     * Remove an entry from a safelist <br>
     * <br>
     * Asking for the removal of an address which is <b>not</b> in the list is not an error and will be silently
     * ignored.
     * 
     * @param safelistOwnerAddress
     *            the logical address owner
     * @param toRemove
     *            the address to remove
     * 
     * @return the contents of the safelist <i>after</i> the operation has been performed
     * 
     * @exception NoSuchSafelistException
     *                Note that this should not be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns
     *                true for this address
     * @exception SafelistActionFailedException
     *                general back-end problem (you're hosed)
     */
    public String[] removeFromSafelist(String safelistOwnerAddress, String toRemove) throws NoSuchSafelistException,
            SafelistActionFailedException;

    /**
     * Remove an entry from multiple safelists.
     * <br>
     * Asking for the removal of an address which is <b>not</b> in the list is not an error and will be silently
     * ignored.
     * 
     * @param safelistOwnerAddress
     *            the logical address owner
     * @param toRemove
     *            Array of addresses to remove.
     * 
     * @return the contents of the safelist <i>after</i> the operation has been performed
     * 
     * @exception NoSuchSafelistException
     *                Note that this should not be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns
     *                true for this address
     * @exception SafelistActionFailedException
     *                general back-end problem (you're hosed)
     */
    public String[] removeFromSafelists(String safelistOwnerAddress, String[] toRemove) throws NoSuchSafelistException,
            SafelistActionFailedException;

    /**
     * Replace a safelist with a new list <br>
     * <br>
     * 
     * @param safelistOwnerAddress
     *            the logical address owner
     * @param listContents
     *            the new list
     * 
     * @return the contents of the safelist <i>after</i> the operation has been performed
     * 
     * @exception NoSuchSafelistException
     *                Note that this should not be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns
     *                true for this address
     * @exception SafelistActionFailedException
     *                general back-end problem (you're hosed)
     */
    public String[] replaceSafelist(String safelistOwnerAddress, String... listContents)
            throws NoSuchSafelistException, SafelistActionFailedException;

    /**
     * If the safelist does not exist yet {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns true, then this
     * method will return an array of length 0.
     * 
     * @param safelistOwnerAddress
     *            the logical address owner
     * 
     * @return the contents of the list
     * 
     * @exception NoSuchSafelistException
     *                Note that this should not be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns
     *                true for this address
     * @exception SafelistActionFailedException
     *                general back-end problem (you're hosed)
     */
    public String[] getSafelistContents(String safelistOwnerAddress) throws NoSuchSafelistException,
            SafelistActionFailedException;

    /**
     * If the safelist does not exist yet {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns true, then this
     * method will return 0 (zero).
     * 
     * @param safelistOwnerAddress
     *            the logical address owner
     * 
     * @return the number of addresses that this owner has safelisted
     * 
     * @exception NoSuchSafelistException
     *                Note that this should not be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns
     *                true for this address
     * @exception SafelistActionFailedException
     *                general back-end problem (you're hosed)
     */
    public int getSafelistCnt(String safelistOwnerAddress) throws NoSuchSafelistException,
            SafelistActionFailedException;

    /**
     * Slightly goofy method (esp the name). This tests if the given address has a safelist, or if one can implicitly be
     * created for the given address. If this method returns true, the methods which manupulate the contents of the
     * safelist should not throw the {@link com.untangle.app.smtp.safelist.NoSuchSafelistException
     * NoSuchSafelistException} they all declare.
     *
     * @param address Address to check.
     * @return true if has or can, false otherwise.
     */
    public boolean hasOrCanHaveSafelist(String address);

    /**
     * Total hack for servlets, to test if a connection is still alive
     */
    public void test();

}
