/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import com.metavize.mvvm.tapi.Pipeline;

class ReleaseTokenStreamer extends TokenStreamer
{
    private final TokenStreamer streamer;
    private final Release release;

    private boolean released = false;

    ReleaseTokenStreamer(Pipeline pipeline, TokenStreamer streamer,
                         Release release)
    {
        super(pipeline);

        this.streamer = streamer;
        this.release = release;
    }

    // IPStreamer methods -----------------------------------------------------

    public boolean closeWhenDone()
    {
        return true;
    }

    // TokenStreamer methods --------------------------------------------------

    protected Token nextToken()
    {
        if (released) {
            return null;
        } else if (null == streamer) {
            released = true;
            return release;
        } else {
            Token t = streamer.nextToken();
            if (null == t) {
                released = true;
                return release;
            } else {
                return t;
            }
        }
    }
}

