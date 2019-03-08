/**
 * $Id$
 */
package com.untangle.uvm.vnet;


/**
 * Flushes a session when parser receives a <code>Release</code>.
 *
 */
class ReleaseTokenStreamer implements TokenStreamer
{
    private final TokenStreamer streamer;
    private final ReleaseToken release;

    private boolean released = false;

    /**
     * ReleaseTokenStreamer constructor
     * @param streamer
     * @param release
     */
    ReleaseTokenStreamer(TokenStreamer streamer, ReleaseToken release)
    {
        this.streamer = streamer;
        this.release = release;
    }

    /**
     * closeWhenDone
     * @return bool
     */
    public boolean closeWhenDone()
    {
        return true;
    }

    /**
     * nextToken - get the next token/chunk
     * @return Token
     */
    public Token nextToken()
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

