/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/quarantine/QuarantineUserView.java $
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

/**
 * Interface for the end-user interface to the Quarantine system.
 */
public interface QuarantineUserView extends QuarantineManipulation
{

    /**
     * Get the account name from an encrypted token. <br>
     * <br>
     * Note that this does <b>not</b> throw NoSuchInboxException.
     */
    public String getAccountFromToken(String token) throws BadTokenException;

    /**
     * Request a digest email to the given address
     * 
     * @param account
     *            the target account
     * 
     * @return true if the digest email could be sent (not nessecerially delivered yet). False if some rules (based on
     *         the address mean that this could never be delivered).
     */
    public boolean requestDigestEmail(String account) throws NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * Request that an email address (inbox) map to another. This is the gesture of someone "giving" their account to
     * someone else (hence, there aren't complex permission problems). <br>
     * Note that it is up to the <b>calling</b> application to ensure the user currently is logged-in as the source of
     * the remap
     * 
     * @param from
     *            the address to be redirected
     * @param to
     *            the target of the redirection
     * 
     * @exception InboxAlreadyRemappedException
     *                If this is a group alias and someone else already created the remap (we follow "first one wins"
     *                semantics).
     * 
     */
    public void remapSelfService(String from, String to) throws QuarantineUserActionFailedException,
            InboxAlreadyRemappedException;

    /**
     * Undoes {@link #remapSelfService remapSelfService}.
     * 
     * @param inboxName
     *            the name of the inbox which is currently <b>receiving</b> the remap.
     * 
     * @param aliasToRemove
     *            the alias to no longer be remapped (and presumably go back to its owner).
     * 
     * @return false if the mapping didn't exist
     */
    public boolean unmapSelfService(String inboxName, String aliasToRemove) throws QuarantineUserActionFailedException;

    /**
     * Test if this address is being remapped to another
     * 
     * @return the address to-which this is remapped, or null if this address is not remapped
     */
    public String getMappedTo(String account) throws QuarantineUserActionFailedException;

    /**
     * List any addresses for-which this account receives redirection.
     * 
     * @return all addresses redirected to this account, or a zero-length array if none are remapped.
     */
    public String[] getMappedFrom(String account) throws QuarantineUserActionFailedException;

}
