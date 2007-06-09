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

package com.untangle.node.token;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Keeps tokens being passed in pipeline.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
final class TokenManager
{
    private static final TokenManager TOKEN_MANAGER = new TokenManager();

    private final Map tokens = new HashMap();
    private final Logger logger = Logger.getLogger(TokenManager.class);

    private long id = 0;

    static TokenManager manager()
    {
        return TOKEN_MANAGER;
    }

    private TokenManager() { }

    // business methods -------------------------------------------------------

    /**
     * Add token to manager. The object will remain in the token
     * manager as long as the key is held onto.
     *
     * @param token object to add.
     * @return the key.
     */
    Long putToken(Token tok)
    {
        Long key;
        synchronized (tokens) {
            key = new Long(++id);
            tokens.put(key, tok);
        }

        return key;
    }

    /**
     * Get token, by key and evict it.
     *
     * @param key token's key.
     * @return the token.
     */
    Token getToken(Long key)
    {
        Token token;
        synchronized (tokens) {
            token = (Token)tokens.remove(key);
        }

        return token;
    }
}
