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

package com.untangle.uvm.vnet.event;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.MPipe;
import com.untangle.uvm.vnet.UDPSession;

/**
 * The class <code>UDPErrorEvent</code> is for events from incoming ICMP messages that are
 * associated with a given UDP session.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class UDPErrorEvent extends UDPPacketEvent {
    
    private byte icmpType;
    private byte icmpCode;
    private InetAddress icmpSource;
    
    public UDPErrorEvent(MPipe mPipe, UDPSession src, ByteBuffer icmpData, IPPacketHeader header, byte icmpType, byte icmpCode, InetAddress icmpSource)
    {
        super(mPipe, src, icmpData, header);
        this.icmpType   = icmpType;
        this.icmpCode   = icmpCode;
        this.icmpSource = icmpSource;
    }

    public byte getErrorType()
    {
        return icmpType;
    }

    public byte getErrorCode()
    {
        return icmpCode;
    }
    
    public InetAddress getErrorSource()
    {
        return icmpSource;
    }
}
