/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/quarantine/QuarantineManipulation.java $
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

import java.util.List;

/**
 * Base-interface for Admin and User views into the Quarantine
 */
public interface QuarantineManipulation
{

    /**
     * Purge the given mails for the named account. <br>
     * <br>
     * If the mails do not exist, this is silently ignored.
     * 
     * @param account
     *            the email address in question
     * @param doomedMails
     *            the {@link com.untangle.node.smtp.quarantine.InboxRecord#getMailID IDs} of the message(s) to be
     *            deleted.
     * 
     * @return the contents of the inbox <i>after</i> the purge operation has taken place
     * 
     * @exception NoSuchInboxException
     *                if there isn't such an inbox (duh)
     * @exception QuarantineUserActionFailedException
     *                if some back-end error occured preventing the operation from successful completion
     */
    public InboxIndex purge(String account, String... doomedMails) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    /**
     * Rescue the given mails for the named account, sending them to <code>account</code>. <br>
     * <br>
     * If the mails do not exist, this is silently ignored.
     * 
     * @param account
     *            the email address in question
     * @param rescuedMails
     *            the {@link com.untangle.node.smtp.quarantine.InboxRecord#getMailID IDs} of the message(s) to be
     *            rescued.
     * 
     * @return the contents of the inbox <i>after</i> the rescue operation has taken place
     * 
     * @exception NoSuchInboxException
     *                if there isn't such an inbox
     * @exception QuarantineUserActionFailedException
     *                if some back-end error occured preventing the operation from successful completion
     */
    public InboxIndex rescue(String account, String... rescuedMails) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    /**
     * Get a listing of the contents of the inbox for <code>account</code>
     * 
     * @param account
     *            the account (email address)
     * 
     * @return the contents of the inbox
     * 
     * @exception NoSuchInboxException
     *                if there isn't such an inbox
     * @exception QuarantineUserActionFailedException
     *                if some back-end error occured preventing the operation from successful completion
     */
    public List<InboxRecord> getInboxRecords(String account) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    /**
     * Total hack for servlets, to test if a connection is still alive
     */
    public void test();

}
