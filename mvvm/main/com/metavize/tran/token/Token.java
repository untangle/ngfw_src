/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Token.java,v 1.2 2005/01/27 09:53:35 amread Exp $
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

/**
 * A pipeline token. This interface will have stuff like toBytes,
 * size, etc...
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface Token
{
    ByteBuffer getBytes();
}
