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

package com.metavize.tran.mail;

import javax.mail.internet.ContentType;

import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import org.apache.log4j.Logger;


public class MimeStateMachine
{
    public enum State {
        START,
        MESSAGE_HEADER,
        BODY,
        PREAMBLE,
        BOUNDARY,
        MULTIPART_HEADER,
        MULTIPART_BODY,
        END_BOUNDARY,
        EPILOG
    };

    private final Logger logger = Logger.getLogger(MimeStateMachine.class);

    private State state = State.START;
    private MimeStateMachine mimeStateMachine = null;

    private Rfc822Header msgHeader = null;
    private Rfc822Header multipartHeader = null;
    private String boundary = null;

    // constructors -----------------------------------------------------------

    public MimeStateMachine() { }

    public MimeStateMachine(String boundary)
    {
        this.state = State.PREAMBLE;
        this.boundary = boundary;
    }

    // public methods ---------------------------------------------------------

    public State nextState(Token token) throws TokenException
    {
        switch (state) {
        case START:
            if (token instanceof Rfc822Header) {
                msgHeader = (Rfc822Header)token;
                state = State.MESSAGE_HEADER;
            } else {
                throw stateException(token);
            }

            return state;

        case MESSAGE_HEADER:
            switch (headerState(msgHeader)) {
            case MESSAGE_HEADER:
                mimeStateMachine = new MimeStateMachine();
                state = State.BODY;
                break;

            case PREAMBLE:
                boundary = mimeBoundary(msgHeader);
                state = State.PREAMBLE;
                break;

            case BODY:
                mimeStateMachine = null;
                state = State.BODY;
                break;

            default:
                throw new IllegalStateException("unexpected state: "
                                                + headerState(msgHeader));
            }

            return null == mimeStateMachine ? state
                : mimeStateMachine.nextState(token);

        case BODY:
            if (null == mimeStateMachine) {
                if (token instanceof Chunk) {
                    state = State.BODY;
                } else {
                    throw stateException(token);
                }
            }

            return null == mimeStateMachine ? state
                : mimeStateMachine.nextState(token);

        case PREAMBLE:
            if (token instanceof Chunk) {
                state = State.PREAMBLE;
            } else if (token instanceof MimeBoundary) {
                MimeBoundary boundary = (MimeBoundary)token;
                if (boundary.isLast()) {
                    throw new TokenException("end boundary after prolog");
                }
                state = State.BOUNDARY;
            } else if (token instanceof Chunk) {
                throw stateException(token);
            }

            return state;

        case BOUNDARY:
            if (token instanceof Rfc822Header) {
                multipartHeader = (Rfc822Header)token;
                state = State.MULTIPART_HEADER;
            } else {
                throw stateException(token);
            }

            return state;

        case MULTIPART_HEADER:
            if (isMatchingBoundary(token)) {
                MimeBoundary b = (MimeBoundary)token;
                state = b.isLast() ? State.END_BOUNDARY : State.BOUNDARY;
                logger.debug("EB STATE? " + state);
                mimeStateMachine = null;
            } else {
                State s = headerState(multipartHeader);
                logger.debug("GOT STATE: " + s);
                if (State.MESSAGE_HEADER == s) {
                    logger.debug("Mimestatemachine()");
                    mimeStateMachine = new MimeStateMachine();
                } else if (State.PREAMBLE == s) {
                    logger.debug("Mimestatemachine(b)");
                    String b = mimeBoundary(multipartHeader);
                    mimeStateMachine = new MimeStateMachine(b);
                } else {
                    logger.debug("Mimestatemachine = null");
                    mimeStateMachine = null;
                }

                state = State.MULTIPART_BODY;
            }

            return null == mimeStateMachine ? state
                : mimeStateMachine.nextState(token);

        case MULTIPART_BODY:
            if (isMatchingBoundary(token)) {
                MimeBoundary b = (MimeBoundary)token;
                state = b.isLast() ? State.END_BOUNDARY : State.BOUNDARY;
                logger.debug("EB STATE? " + state);
                mimeStateMachine = null;
            }

            return null == mimeStateMachine ? state
                : mimeStateMachine.nextState(token);

        case END_BOUNDARY:
            if (token instanceof Chunk) {
                state = State.EPILOG;
            } else {
                throw stateException(token);
            }

            return state;

        case EPILOG:
            if (token instanceof Chunk) {
                state = State.EPILOG;
            } else {
                throw stateException(token);
            }

            return state;

        default:
            throw new IllegalStateException("unknown state: " + state);

        }
    }

    // private methods --------------------------------------------------------

    private TokenException stateException(Token token)
    {
        return new TokenException("bad token: " + token.getClass()
                                  + " in state; " + state);
    }

    private State headerState(Rfc822Header header) throws TokenException
    {
        String mimeVersion = header.getMimeVersion();

        ContentType contentType;
        try {
            contentType = header.getContentType();
        } catch (ParseException exn) {
            throw new TokenException(exn);
        }

        State state;

        if (null == contentType) {
            logger.warn("MIME-Version without Content-Type");
            state = State.BODY;
        } else {
            String pType = contentType.getPrimaryType();
            String sType = contentType.getSubType();

            if (pType.equals("multipart")) {
                state = State.PREAMBLE;
            } else if (pType.equals("message") && sType.equals("rfc822")) {
                state = State.MESSAGE_HEADER;
            } else {
                state = State.BODY;
            }
        }

        return state;
    }

    private String mimeBoundary(Rfc822Header header) throws TokenException
    {
        ContentType contentType;
        try {
            contentType = header.getContentType();
        } catch (ParseException exn) {
            throw new TokenException(exn);
        }
        return contentType.getParameter("boundary");
    }

    private boolean isMatchingBoundary(Token token)
    {
        if (token instanceof MimeBoundary) {
            logger.debug("OURS: " + boundary + "TOK: " + token);
        }

        return token instanceof MimeBoundary
            && ((MimeBoundary)token).getDelimiter().equals(boundary);
    }
}
