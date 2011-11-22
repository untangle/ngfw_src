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

package com.untangle.node.mail.papi.imap;

import com.untangle.node.mail.papi.ContinuedMIMEToken;
import com.untangle.node.token.TokenResult;


/**
 * Abstract base class for Objects wishing to listen
 * for callbacks on a {@link com.untangle.node.mail.papi.imap.ImapTokenStream ImapTokenStream}.
 * Hopefully, the method names should make sense vis-a-vis their use.  The
 * only "trick" is that mails will either arrive as:
 * <ul>
 *   <li>
 *       A call to {@link #handleBeginMIMEFromServer handleBeginMIMEFromServer}
 *       followed by one or more calls to
 *       {@link #handleContinuedMIMEFromServer handleContinuedMIMEFromServer}.
 *   </li>
 *   <li>
 *     A single call to {@link #handleCompleteMIMEFromServer handleCompleteMIMEFromServer}.
 *   </li>
 * </ul>
 * <br><br>
 * Note that all working methods are abstract (instead of
 * with a passthru implementation).  This is to prevent goofups
 * if new methods are added (and not implemented).  If someone
 * wants a passthru implementation, one has
 * {@link com.untangle.node.mail.papi.imap.PassthruImapTokenStreamHandler already been created}.
 */
public abstract class ImapTokenStreamHandler {

    private ImapTokenStream m_its;

    public final void setStream(ImapTokenStream stream) {
        m_its = stream;
    }

    /**
     * Get the ImapTokenStream currently using this handler.  Note that
     * this may return null if the handler is not currently
     * registered with a Stream.
     */
    protected final ImapTokenStream getStream() {
        return m_its;
    }

    /**
     * Handle a FIN <b>from</b> the client.
     * <br><br>
     * Returning true means "go ahead and close the server".
     *
     * @return true if this should be propigated to the next
     *         node/unparser
     */
    public abstract boolean handleClientFin();

    /**
     * Handle a FIN <b>from</b> the server.
     * <br><br>
     * Returning true means "go ahead and close the client".
     *
     * @return true if this should be propigated to the next
     *         node/unparser
     */
    public abstract boolean handleServerFin();

    /**
     * Handle a chunk of Imap protocol bytes from the server to the
     * client.  Note that this is stuff <b>other than</b>
     * MIME data.
     */
    public abstract TokenResult handleChunkFromServer(ImapChunk token);
    public abstract TokenResult handleBeginMIMEFromServer(BeginImapMIMEToken token);
    public abstract TokenResult handleContinuedMIMEFromServer(ContinuedMIMEToken token);
    public abstract TokenResult handleCompleteMIMEFromServer(CompleteImapMIMEToken token);
    public abstract void handleFinalized();

}

