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
package com.untangle.node.mail.papi.safelist;

import java.util.List;

import com.untangle.node.mime.EmailAddress;

/**
 * Interface for the nodes to query the
 * safelist.  This is not intended to be
 * "remoted" to any UI.
 */
public interface SafelistNodeView {

    /**
     * Test if the given sender is safelisted
     * for the given recipients.  Implementations
     * of Safelist are permitted to ignore the
     * recipient set and simply maintain a "global"
     * list.
     * <br><br>
     * Note that if separate Safelists are maintained
     * for each recipient, this method should return
     * <code>true</code> if <b>any</b> of the recipients
     * have declared the sender on a safelist.
     *
     * @param envelopeSender the sender of the message
     *        as declared on the envelop (SMTP-only).  Obviously,
     *        this may be null
     * @param mimeFrom the sender of the email, as declared on
     *        the FROM header of the MIME message.  May be null,
     *        but obviously if this <b>and</b> the envelope
     *        sender are null false will be returned.
     * @param recipients the recipient(s) of the message
     *
     * @return true if the sender (either the envelope
     *         or MIME) is safelisted.  False does not mean
     *         that the sender is blacklisted - just not safelisted
     *
     */
    public boolean isSafelisted(EmailAddress envelopeSender,
                                EmailAddress mimeFrom,
                                List<EmailAddress> recipients);


}
