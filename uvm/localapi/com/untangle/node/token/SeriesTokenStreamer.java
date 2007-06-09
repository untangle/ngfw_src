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

package com.untangle.tran.token;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

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
