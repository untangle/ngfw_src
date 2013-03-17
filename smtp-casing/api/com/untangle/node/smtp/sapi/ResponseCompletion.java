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

import com.untangle.node.smtp.Response;


/**
 * Callback interface for Object wishing to know
 * a Response has returned from the server.
 * ResponseCompletion instances are associated
 * with a Response via the handleXXXXXXX methods
 * on
 * {@link com.untangle.node.smtp.sapi.SessionHandler SessionHandler}
 * and
 * {@link com.untangle.node.smtp.sapi.TransactionHandler TransactionHandler}
 * <br>
 * The original Command is <b>not</b> passed into the callback method
 * on this interface.  Instances of ResponseCompletion which need to
 * know with which Command they are associated should be constructed
 * to "remember" this.
 */
public interface ResponseCompletion {


    /**
     * Handle a response.  The Response is <b>not</b>
     * automatically passed back to the client.  If the
     * Completion wishes to pass the Response back
     * through to the client they should use
     * <code>
     * actions.sendResponseToClient(resp);
     * </code>
     * <br>
     * If the Request was synthetic (i.e. issued by the Handler,
     * not the real client) then the response should be supressed.
     * To supress a response from flowing back to the client
     * take no action.
     *
     * @param resp the response from Server
     * @param actions the set of available actions.
     */
    public void handleResponse(Response resp,
                               Session.SmtpResponseActions actions);

}
