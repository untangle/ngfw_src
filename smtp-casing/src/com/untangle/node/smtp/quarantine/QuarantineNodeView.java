/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/quarantine/QuarantineNodeView.java $
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

import java.io.File;

import javax.mail.internet.InternetAddress;

/**
 * Interface for the nodes to insert messages into the quarantine. This is not intended to be "remoted".
 */
public interface QuarantineNodeView
{

    /**
     * Quarantine the given message, destined for the named recipients. <br>
     * <br>
     * Callers should be prepared for the case that after making this call, the underlying File from the MIMEMessage may
     * have been "stolen" (moved).
     * 
     * @param file
     *            the file containing the message to be quarantined
     * @param summary
     *            a summary of the mail
     * @param recipients
     *            any recipients for the mail
     * 
     * @return true if the mail was quarantined.
     */
    public boolean quarantineMail(File file, MailSummary summary, InternetAddress... recipients);

    public String createAuthToken(String account);
}