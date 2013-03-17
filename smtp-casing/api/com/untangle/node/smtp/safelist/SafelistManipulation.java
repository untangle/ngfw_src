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

package com.untangle.node.smtp.safelist;

/**
 * Base-interface for Admin and User views
 * into the Safelist Manager
 */
public interface SafelistManipulation {

    /**
     * Add an entry to a safelist
     * <br><br>
     * Requesting the addition of a duplicate address is
     * not an error and will be silently ignored.
     *
     * @param safelistOwnerAddress the logical address owner
     * @param toAdd the address to add
     *
     * @return the contents of the safelist <i>after</i> the operation
     *         has been performed
     *
     * @exception NoSuchSafelistException Note that this should not
     *            be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist}
     *            returns true for this address
     * @exception SafelistActionFailedException general back-end problem (you're hosed)
     */
    public String[] addToSafelist(String safelistOwnerAddress,
                                  String toAdd)
        throws NoSuchSafelistException, SafelistActionFailedException;

    /**
     * Remove an entry from a safelist
     * <br><br>
     * Asking for the removal of an address which is <b>not</b>
     * in the list is not an error and will be silently ignored.
     *
     * @param safelistOwnerAddress the logical address owner
     * @param toRemove the address to remove
     *
     * @return the contents of the safelist <i>after</i> the operation
     *         has been performed
     *
     * @exception NoSuchSafelistException Note that this should not
     *            be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist}
     *            returns true for this address
     * @exception SafelistActionFailedException general back-end problem (you're hosed)
     */
    public String[] removeFromSafelist(String safelistOwnerAddress,
                                       String toRemove)
        throws NoSuchSafelistException, SafelistActionFailedException;

    public String[] removeFromSafelists(String safelistOwnerAddress,
            String[] toRemove)
        throws NoSuchSafelistException, SafelistActionFailedException;
    
    /**
     * Replace a safelist with a new list
     * <br><br>
     *
     * @param safelistOwnerAddress the logical address owner
     * @param listContents the new list
     *
     * @return the contents of the safelist <i>after</i> the operation
     *         has been performed
     *
     * @exception NoSuchSafelistException Note that this should not
     *            be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist}
     *            returns true for this address
     * @exception SafelistActionFailedException general back-end problem (you're hosed)
     */
    public String[] replaceSafelist(String safelistOwnerAddress,
                                    String...listContents)
        throws NoSuchSafelistException, SafelistActionFailedException;

    /**
     * If the safelist does not exist yet
     * {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns true,
     * then this method will return an array of length 0.
     *
     * @param safelistOwnerAddress the logical address owner
     *
     * @return the contents of the list
     *
     * @exception NoSuchSafelistException Note that this should not
     *            be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist}
     *            returns true for this address
     * @exception SafelistActionFailedException general back-end problem (you're hosed)
     */
    public String[] getSafelistContents(String safelistOwnerAddress)
        throws NoSuchSafelistException, SafelistActionFailedException;

    /**
     * If the safelist does not exist yet
     * {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist} returns true,
     * then this method will return 0 (zero).
     *
     * @param safelistOwnerAddress the logical address owner
     *
     * @return the number of addresses that this owner has safelisted
     *
     * @exception NoSuchSafelistException Note that this should not
     *            be thrown if {@link #hasOrCanHaveSafelist hasOrCanHaveSafelist}
     *            returns true for this address
     * @exception SafelistActionFailedException general back-end problem (you're hosed)
     */
    public int getSafelistCnt(String safelistOwnerAddress)
        throws NoSuchSafelistException, SafelistActionFailedException;

    /**
     * Slightly goofy method (esp the name).  This tests if the given
     * address has a safelist, or if one can implicitly be created
     * for the given address.  If this method returns true, the methods
     * which manupulate the contents of the safelist should not throw
     * the {@link com.untangle.node.smtp.safelist.NoSuchSafelistException NoSuchSafelistException}
     * they all declare.
     */
    public boolean hasOrCanHaveSafelist(String address);

    /**
     * Total hack for servlets, to test if a connection is still alive
     */
    public void test();

}
