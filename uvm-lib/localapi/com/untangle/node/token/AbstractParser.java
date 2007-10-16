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

import com.untangle.uvm.vnet.TCPSession;

/**
 * Abstract base class for parsers.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class AbstractParser implements Parser
{
    private final String idStr;

    protected final TCPSession session;
    protected final boolean clientSide;

    // constructors -----------------------------------------------------------

    protected AbstractParser(TCPSession session, boolean clientSide)
    {
        this.session = session;
        this.clientSide = clientSide;

        String name = getClass().getName();

        this.idStr = name + "<" + (clientSide ? "CS" : "SS") + ":"
            + session.id() + ">";
    }

    /**
     * Get the underlying Session associated with this parser
     *
     * @return the session.
     */
    protected TCPSession getSession() {
        return session;
    }

    // Parser noops -----------------------------------------------------------

    public TokenStreamer endSession() { return null; }

    // session manipulation ---------------------------------------------------

    protected void lineBuffering(boolean oneLine)
    {
        if (clientSide) {
            session.clientLineBuffering(oneLine);
        } else {
            session.serverLineBuffering(oneLine);
        }
    }

    protected int readLimit()
    {
        if (clientSide) {
            return session.clientReadLimit();
        } else {
            return session.serverReadLimit();
        }
    }

    protected void readLimit(int limit)
    {
        if (clientSide) {
            session.clientReadLimit(limit);
        } else {
            session.serverReadLimit(limit);
        }
    }

    protected void scheduleTimer(long delay)
    {
        session.scheduleTimer(delay);
    }

    protected void cancelTimer()
    {
        session.cancelTimer();
    }

    protected boolean isClientSide() {
        return clientSide;
    }

    // no-ops methods ---------------------------------------------------------

    public void handleTimer() { }
    public void handleFinalized() { }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return idStr;
    }


}
