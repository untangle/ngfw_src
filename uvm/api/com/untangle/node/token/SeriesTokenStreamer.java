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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

/**
 * Streams out <code>Token</code>s from a series of
 * <code>TokenStreamer</code>s.
 *
 */
public class SeriesTokenStreamer implements TokenStreamer
{
    private final List<TokenStreamer> streamers;
    private final Iterator<TokenStreamer> i;

    private TokenStreamer streamer;

    // Constructors -----------------------------------------------------------

    public SeriesTokenStreamer(TokenStreamer ts0, TokenStreamer ts1)
    {
        this.streamers = new ArrayList<TokenStreamer>(2);
        streamers.add(ts0);
        streamers.add(ts1);

        this.i = streamers.iterator();
    }

    public SeriesTokenStreamer(List<TokenStreamer> streamers)
    {
        this.streamers = streamers instanceof RandomAccess ? streamers
            : new ArrayList<TokenStreamer>(streamers);
        this.i = streamers.iterator();
    }

    // TokenStreamer methods --------------------------------------------------

    public Token nextToken()
    {
        if (!i.hasNext()) {
            return null;
        } else if (null == streamer) {
            streamer = i.next();
        }

        Token t = null;
        while (null == t) {
            t = streamer.nextToken();
            if (null == t) {
                if (i.hasNext()) {
                    streamer = i.next();
                } else {
                    break;
                }
            }
        }

        return t;
    }

    public boolean closeWhenDone()
    {
        return streamers.get(streamers.size() - 1).closeWhenDone();
    }
}
