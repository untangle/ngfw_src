/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TCPSessionImpl.java,v 1.1 2005/01/03 20:42:59 rbscott Exp $
 */

package com.metavize.mvvm.argon;

import com.metavize.jnetcap.*;

public class TCPSessionImpl extends IPSessionImpl implements TCPSession 
{
    public TCPSessionImpl( TCPNewSessionRequest request )
    {
        super( request );
    }
}
