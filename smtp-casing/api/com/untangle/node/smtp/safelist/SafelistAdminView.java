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
import java.util.List;

/**
 * Interface for Admins to browse/manipulate
 * the Quarantine.
 */
public interface SafelistAdminView
    extends SafelistManipulation {

    /**
     * List the email addresses of all users who
     * have a safelist
     *
     * @return the list of all safelist owners.
     */
    public List<String> listSafelists()
        throws SafelistActionFailedException;

    /**
     * Delete the safelist for the given
     * email address.  Note that if there is no
     * such account, this error is ignored.
     *
     * @param safelistOwnerAddress the email address
     */
    public void deleteSafelist(String safelistOwnerAddress)
        throws SafelistActionFailedException;
    
    public void deleteSafelists(String[] safelistOwnerAddresses)
        throws SafelistActionFailedException;

    /**
     * To avoid any anoying concurrency issues, callers are
     * permitted to attempt to create an existing safelist.  In
     * other words, the implementation must perform any
     * synchronization to prevent ill effects of duplicate
     * list creation.
     */
    public void createSafelist(String newListOwnerAddress)
        throws SafelistActionFailedException;

    /**
     * Test if the given Safelist exists.
     *
     * @param safelistOwnerAddress
     *
     * @return true if address exists
     */
    public boolean safelistExists(String safelistOwnerAddress)
        throws SafelistActionFailedException;

    /**
     * List the email addresses and counts of all users who
     * have a safelist
     *
     * @return the list of all safelist owners counts.
     */
    public List<SafelistCount> getUserSafelistCounts()
        throws NoSuchSafelistException, SafelistActionFailedException;

}
