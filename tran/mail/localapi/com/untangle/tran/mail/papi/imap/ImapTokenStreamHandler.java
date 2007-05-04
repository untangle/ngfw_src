/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.papi.imap;

import com.untangle.tran.mail.papi.ContinuedMIMEToken;
import com.untangle.tran.token.TokenResult;


/**
 * Abstract base class for Objects wishing to listen
 * for callbacks on a {@link com.untangle.tran.mail.papi.imap.ImapTokenStream ImapTokenStream}.
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
 * {@link com.untangle.tran.mail.papi.imap.PassthruImapTokenStreamHandler already been created}.
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
     *         transform/unparser
     */
    public abstract boolean handleClientFin();

    /**
     * Handle a FIN <b>from</b> the server.
     * <br><br>
     * Returning true means "go ahead and close the client".
     *
     * @return true if this should be propigated to the next
     *         transform/unparser
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

