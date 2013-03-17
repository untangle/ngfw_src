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

package com.untangle.node.smtp.sapi;

import com.untangle.node.smtp.Command;
import com.untangle.node.smtp.Response;
import com.untangle.node.smtp.SmtpTransaction;


/**
 * Root callback abstract class for Object wishing to
 * participate in an Smtp Session.  A SessionHandler
 * is passed to the constructor of a
 * {@link com.untangle.node.smtp.sapi.Session Session}.
 * <br>
 * Once registered with a Session, the SessionHandler will be
 * called back to handle various transitions within an Smtp
 * Session.
 * <br>
 * This inabstract class and the associated interfaces instances must
 * obey (such as TransactionHandlers) work from the following
 * model.
 * <br>
 * Client Commands (as well as various forms of MIME Messages}
 * are received by the {@link com.untangle.node.smtp.sapi.Session parent Session}.
 * These are then passed to either instances of this interface, or
 * to {@link com.untangle.node.smtp.sapi.TransactionHandler TransactionHandlers}.
 * The Session maintains Transaction boundaries, calling the
 * {@link #createTxHandler factory method} to create TransactionHandlers are
 * Transactions are entered.
 * <br>
 * Each Command/MIME Bit are then passed to a callback on the Session/TransactionHandler
 * along with an Object representing the available Actions (@see Session).  Handlers
 * then can either pass-along the Command/MIME Bit or perform some protocol manipulation.
 * When a Command is passed-along to the server, the Handler must also provide an Object
 * to be notified when the response arrives from the server.  This is a
 * {@link com.untangle.node.smtp.sapi.ResponseCompletion ResponseCompletion}.
 * For every command issued to the server there should be one outstanding
 * ResponseCompletion.  The Session maintains ordering, such that any pipelining
 * performed by the client is "hidden" from implementers of this interface.
 * <br>
 * For the purposes of protocol manipulation, we will define
 * three terms:
 * <ul>
 *   <li>
 *     <i>Synthetic Request</i>.  This is a request issued to the
 *        server by the Handler itself.  It did not originate from
 *        the client.
 *   </li>
 *   <li>
 *     <i>Synthetic Response</i>.  This is a response issued by the handler
 *        as a result of a client request.  The server never "sees" the
 *        request, and instead the Handler acts-as the server.
 *   </li>
 *   <li>
 *     <i>Buffering</i>.  This form of manipulation takes data from the
 *        client yet does not pass it along to the server.
 *   </li>
 * </ul>
 *
 */
public abstract class SessionHandler {

    private Session m_session;

    /**
     * <b>Only to be called by the Session passing
     * itself</b>
     */
    protected final void setSession(Session stream) {
        m_session = stream;
    }

    /**
     * Get the Session which is calling this class during
     * an SMTP Session.   If this instance is not registered
     * with any Session, this method
     * returns null.
     */
    public final Session getSession() {
        return m_session;
    }


    /**
     * Handle a "normal" Command from the client.  This
     * includes things like "HELO" or "HELP" issued
     * outside the boundaries of a transaction.
     * <br><br>
     * Note that it may be confusing, but a client
     * can issue a "RSET" when not within a transaction.
     * This is a "safety" thing, for servers which
     * reuse Mail sessions (and want the session in
     * a known state before begining a new Transaction).
     * <br><br>
     * Note also that because of the Session's built-in
     * manipulation of the EHLO command, the EHLO
     * command will never be sent to this method.
     * Instead, the EHLO Command is passed to the
     * {@link #observeEHLOCommand observeEHLOCommand()}
     * method which can optionally be overidden.
     *
     * @param command the client command
     * @param actions the available actions
     */
    public abstract void handleCommand(Command command,
                                       Session.SmtpCommandActions actions);

    /**
     * Edge-case handler.  When an SMTP Session is created,
     * the server is the first actor to send data.  However,
     * the SessionHandler did not "see" any request corresponding
     * to this response and could not have installed
     * a ResponseCompletion.  Instead, this method is used
     * to handle this "misaligned" response.
     *
     * This built-in method may be overriden.
     *
     * @param resp the response
     * @param actions the available actions.
     */
    public void handleOpeningResponse(Response resp,
				      Session.SmtpResponseActions actions) {
        actions.sendResponseToClient(resp);
    }


    /**
     * The calling {@link #getSession Session} handles
     * the EHLO command and its response, to ensure
     * {@link com.untangle.node.smtp.sapi.Session#setAllowedExtensions only allowed extensions}
     * are seen by the client.  As-such, subclasses do
     * <b>not</b> have a chance to explicitly manipulate this
     * portion of the protocol.  However, for logging purposes
     * the handler is shown the EHLO line
     * <br><br>
     * Default implementation does nothing
     *
     * @param cmd the EHLO command
     */
    public void observeEHLOCommand(Command cmd) {
        //Do nothing
    }

    /**
     * Chance for subclasses to manipulate the EHLO response.  Note
     * that the EHLO response has already been altered to ensure
     * {@link com.untangle.node.smtp.sapi.Session#setAllowedExtensions only allowed extensions}
     * are advertized.  Subclasses may wish to further reduce available
     * extensions, or simply to log what transpired.
     * <br><br>
     * Default implementation simply returns the argument (does nothing).
     *
     * @param resp the Response from server, already manipulated by
     *        the {@link @getSession session}
     * @return the manipualted response, or <code>resp</code>
     *         if no manipulation is to take place.
     */
    public Response manipulateEHLOResponse(Response resp) {
        return resp;
    }

    /**
     * Create a new TransactionHandler.  This method is called
     * as the Session crosses a Transaction boundary.  Note that
     * the previous Transaction may still be incomplete (waiting
     * for final server disposition) when pipelining (either legal
     * or otherwise) is employed by the client.
     *
     * @param tx the Transaction to be associated with the Handler
     * @return a new TransactionHandler
     */
    public abstract TransactionHandler createTxHandler(SmtpTransaction tx);


    /**
     * Handle a FIN from the server.
     *
     * @param currentTX the current transaction (if there is one open).
     *
     * @return true if the client should be shutdown.  False
     *         to leave the client side open.
     */
    public abstract boolean handleServerFIN(TransactionHandler currentTX);

    /**
     * Handle a FIN from the client.
     *
     * @param currentTX the current transaction (if there is one open).
     *
     * @return true if the server should be shutdown.  False
     *         to leave the server side open.
     */
    public abstract boolean handleClientFIN(TransactionHandler currentTX);

    /**
     * Called when both client and server sides are closed.  Any
     * associated resources should be closed and any interesting logging
     * made.
     */
    public abstract void handleFinalized();

}
