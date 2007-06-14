/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import java.util.LinkedList;

import com.untangle.jvector.*;
import org.apache.log4j.BasicConfigurator;

public class Test
{
    private static final int NUMTRAN = 1000;

    public static void main (String[] args)
    {
        IncomingSocketQueue[] incoming = new IncomingSocketQueue[NUMTRAN];
        OutgoingSocketQueue[] outgoing = new OutgoingSocketQueue[NUMTRAN];
        Relay[] relay = new Relay[NUMTRAN];
        Transform[] transform = new Transform[NUMTRAN];
        LinkedList relays = new LinkedList();
        int i;

        /* Setup log4j */
        BasicConfigurator.configure();

        for (i=0;i<NUMTRAN;i++) {
            incoming[i] = new IncomingSocketQueue();
            outgoing[i] = new OutgoingSocketQueue();
            transform[i] = new Transform(incoming[i], outgoing[i]);
            transform[i].name(new String ("Transform" + i));
        }
        for (i=0;i<NUMTRAN-1;i++) {
            relay[i] = new Relay(outgoing[i],incoming[i+1]);
            relays.add(relay[i]);
        }

        Vector vec = new Vector(relays);
        Crumb  obj = new DataCrumb( new byte[] { 1 } );
        // Object obj = new Integer(1);

        System.out.println("Sending Object: " + obj);

        incoming[0].sq().add(obj);

        vec.timeout( 1 );
        vec.vector();

        return;
    }

    //     public void event ( SocketQueue obj)
    //     {
    //         System.out.println("Got     Object: " + obj);
    //     }

    static {
        System.loadLibrary("uvmcore");
    }
}
