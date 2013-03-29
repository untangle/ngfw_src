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

package com.untangle.node.token;

import com.untangle.uvm.vnet.event.TCPStreamer;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPChunkResult;
import java.nio.ByteBuffer;

/**
 * An Unparser nodes tokens into bytes.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface Unparser
{
    /**
     * Node tokens back into bytes.
     *
     * @param token next token.
     * @return UnparseResult containing byte content of token.
     * @exception UnparseException on unparse error.
     */
    UnparseResult unparse(Token token) throws UnparseException;

    /**
     * Used for casings that expect byte stream on both sides
     *
     * @param chunk next of data
     * @return UnparseResult containing unparsed content of the chunk
     * @exception UnparseException on unparse error.
     */
    ByteBuffer unparse(TCPChunkEvent event) throws UnparseException;

    /**
     * Called when a session is being released. The unparser should
     * return any queued data.
     *
     * @return Unparse result containing queued data.
     * @exception UnparseException if thrown, it will cause the
     * session to be closed.
     */
    UnparseResult releaseFlush() throws UnparseException;

    /**
     * On session end, the unparser has an opportunity to stream data.
     *
     * @return TokenStreamer that streams the final data.
     */
    TCPStreamer endSession();

    /**
     * Called when both client and server sides
     * {@link com.untangle.uvm.vnet.event.SessionEventListener#handleTCPFinalized are shutdown}
     */
    void handleFinalized();
}
