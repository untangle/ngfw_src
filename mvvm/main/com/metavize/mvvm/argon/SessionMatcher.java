/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: SessionMatcher.java,v 1.1 2005/02/10 00:44:54 rbscott Exp $
 */

package com.metavize.mvvm.argon;

public interface SessionMatcher
{
    /**
     * Tells if the session matches */
    boolean isMatch( IPSessionDesc session );    
}
