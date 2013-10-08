/*
 * $HeadURL: svn://chef/work/src/jnetcap/test/Test.java $
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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import com.untangle.jnetcap.*;
import com.untangle.jvector.*;
import org.apache.log4j.BasicConfigurator;

public class Test {
    private static final String inetConverterFlag = "InetConverter";
    private static final String ipFlag            = "IP";
    private static final String subscribeFlag     = "Subscribe";
    private static final String verboseFlag       = "verbose";

    private static final String usage = "::::::Usage:::::\n\n\t<test-type> <test-args>\n" +
        "\ttest-type: '" + inetConverterFlag + "', test-args: space separated list of hosts/IP addresss\n" +
        "\ttest-type: '" + ipFlag + "', print out the ip configuration (no args)\n" +
        "\ttest-type: '" + subscribeFlag     + "', test-args: [<number of transforms, defaults to 10>] " +
        "[" + verboseFlag + "]\n";

    private static final SubscriptionManager subManager = new SubscriptionManager();

    public static void usage()
    {
        System.out.println( usage );
        System.exit( 1 );
    }

    public static void main ( String args[] )
    {
        if ( args.length == 0 ) usage();

        if ( args[0].equals( inetConverterFlag )) {
            inetConverter ( args );
        } else if ( args[0].equals( subscribeFlag )) {
            subscribeTest ( args );
        } else if ( args[0].equals( ipFlag )) {
            ipTest( args );
        } else {
            usage();
        }
    }

    private static void inetConverter ( String args[] )
    {
        int length = args.length;

        try {
            for ( int c = 1 ; c < length ; c ++ ) {
                InetAddress test = Inet4Address.getByName ( args[c] );
                byte testArray[] = test.getAddress();
                long result;

                System.out.println ( "This is a test: " +  test.getHostAddress() );

                result = Inet4AddressConverter.toLong ( test );

                System.out.println ( "toLong: " + Long.toHexString ( result ));

                System.out.println( "toAddress: " + Inet4AddressConverter.toAddress( result ).getHostAddress());
            }

            Random generator = new Random();
            boolean passed = true;
            for ( int c = 0 ; c < 1000 ; c++ ) {
                long val = generator.nextLong();
                long convert = Inet4AddressConverter.toLong( Inet4AddressConverter.toAddress( val ));

                if ( (int)val != (int)convert ) {
                    System.out.println( "Error in conversion("+ c + "): " + Long.toHexString( val ) + "|" +
                                        Long.toHexString( convert ));
                    passed = false;
                    break;
                }
            }
            if ( passed ) System.out.println( "Test passed" );
            else System.out.println( "Test failed" );
        } catch ( UnknownHostException err ) {
            System.out.println ( "Unknown host: " + err.getMessage() );
        }
    }

    private static void ipTest( String args[] ) {
        System.out.println( "\n\nHOST CONFIGURATION:\n" );
        System.out.println( "IP      " + Netcap.getHost());
        System.out.println( "NETMASK " + Netcap.getNetmask());
        System.out.println( "GATEWAY " + Netcap.getGateway());
    }

    private static void subscribeTest ( String args[] ) {
        int numTransforms = 10;
        boolean verbose = false;

        if ( args.length > 3 ) usage();

        for ( int c = 1 ; c < args.length ; c++ ) {
            if ( args[c].equalsIgnoreCase( verboseFlag )) {
                if ( verbose == true ) usage();
                verbose = true;
            } else {
                try {
                    numTransforms = Integer.parseInt( args[c] );
                } catch ( NumberFormatException e ) {
                    System.err.println( "Unable to parse: " + args[c] );
                    usage();
                }

                if ( numTransforms < 0 ) usage();
            }
        }

        if ( verbose ) {
            BasicConfigurator.configure();
        }

        /* Enable the shield with a debugging level of 5 */
        Netcap.init( true, 5 );

        /* Install two guards, who lets everything except ssh, just not SSH for testing */

        /* If this is verbose then enable it */
        if ( verbose ) {
            /* Increase the debug level to 10 */
            Netcap.debugLevel( 10 );
            Vector.jvectorDebugLevel( 0 );
            Vector.mvutilDebugLevel( 0 );
            Vector.vectorDebugLevel( 0 );
        } else {
            Netcap.debugLevel( 1 );
            Vector.jvectorDebugLevel( 0 );
            Vector.mvutilDebugLevel( 0 );
            Vector.vectorDebugLevel( 0 );
        }

        /* Donate a few threads */
        Netcap.donateThreads( 10 );

        /* Start the scheduler */
        Netcap.startScheduler();

        /* XXX How do I make TestUDPHook a singleton that has one instance */
        Netcap.registerUDPHook( new TestUDPHook( numTransforms, verbose ));

        Netcap.registerTCPHook( new TestTCPHook( numTransforms, verbose ));

        /* Make one subscription */
        /* Test out the local antisubscribe */
        SubscriptionGenerator gen = new SubscriptionGenerator( Netcap.IPPROTO_UDP );

        gen.server().port( new Range( 60000, 60001 ));
        //gen.server().port( new Range( 7, 7 ));
        subManager.add( gen.subscribe());

        gen.server().port( 7 );
        // gen.server().port( new Range( 7, 7 ));

        subManager.add( gen.subscribe());

        gen = new SubscriptionGenerator( Netcap.IPPROTO_TCP );
        // Disable local antisubscribes
        // gen = new SubscriptionGenerator( Netcap.IPPROTO_TCP, SubscriptionGenerator.DEFAULT_FLAGS |
        //                                          SubscriptionGenerator.LOCAL_ANTI_SUBSCRIBE );


        /* Port 60000 will direct through the header transform. */
        /* Port 60001 will direct through the TestHalfWriteTransform */
        gen.server().port( new Range( 60000, 60001 ));
        //gen.client().address( Inet4AddressConverter.getByAddress ( new int[] { 10, 0, 0, 198 } ));
        //gen.client().netmask( Inet4AddressConverter.getByAddress ( new int[] { 255, 255, 255, 254 } ));
        subManager.add( gen.subscribe());

        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable() {
                public void run() {
                    /* Remove all of the subscriptions */
                    subManager.unsubscribeAll();
                    Netcap.cleanup();
                }
            } ));

    }
}
