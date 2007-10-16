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
