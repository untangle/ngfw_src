/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: MutableEndpoint.java,v 1.3 2005/01/03 23:34:33 rbscott Exp $
 */


package com.metavize.jnetcap;

import java.net.InetAddress;

public interface MutableEndpoint extends Endpoint {
    public void host( InetAddress newHost );
    public void port( int newPort );
    public void interfaceName( String newInterfaceName );
    public void interfaceId( byte newInterfaceId );
}
