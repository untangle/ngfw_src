/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn;

import java.util.List;
import java.util.LinkedList;

import java.util.Collections;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tran.ValidateException;

class AddressValidator
{
    private static final Logger logger = Logger.getLogger( AddressValidator.class );

    private static List<AddressRange> ILLEGAL_ADDRESS_LIST = new LinkedList<AddressRange>();
    
    AddressValidator()
    {
    }

    void validate( List<AddressRange> addressRangeList ) throws ValidateException
    {
        /* Make sure that none of these overlap */
        List<AddressRange> checkList = new LinkedList<AddressRange>( addressRangeList );
        checkList.addAll( ILLEGAL_ADDRESS_LIST );
        
        /* Sort the list */
        Collections.sort( checkList );
        
        /* Check for overlap */
        AddressRange previous = null;
        for ( AddressRange range : checkList ) {
            if ( previous != null ) {
                logger.debug( "Checking address range: " + range.getDescription() + 
                              " against " + previous.getDescription());

                if ( range.getStart() < previous.getEnd()) {
                    if ( range.getIsIllegal() && previous.getIsIllegal()) {
                        logger.warn( "Overlapping in the list of illegal addresses: " + 
                                     range.getDescription() + "," + previous.getDescription());
                    }
                    
                    if ( range.getIsIllegal()) {
                        throw new 
                            ValidateException( "The network: " + previous.getDescription() +
                                               " cannot overlap with the network " + 
                                               range.getDescription()); 

                    } else if ( previous.getIsIllegal()) {
                        throw new 
                            ValidateException( "The network: " + range.getDescription() +
                                               " cannot overlap with the network: " + 
                                               previous.getDescription());

                        /* They are both not illegal */
                    } else {
                        throw new 
                            ValidateException( "The two networks: " + range.getDescription() +
                                               " and " + previous.getDescription() + " cannot overlap" );
                    }
                }
            }
            
            previous = range;
        }
    }

    static {
        try {
            /* This list is from RFC 3330 */
            /* This network */
            ILLEGAL_ADDRESS_LIST.add( AddressRange.makeNetwork( InetAddress.getByName( "127.0.0.0" ),
                                                                InetAddress.getByName( "255.0.0.0" ),
                                                                true ));
            
            /* Loopback */
            ILLEGAL_ADDRESS_LIST.add( AddressRange.makeNetwork( InetAddress.getByName( "0.0.0.0" ),
                                                                InetAddress.getByName( "255.0.0.0" ),
                                                                true ));
            
            /* Link local (unassinged machines) */
            ILLEGAL_ADDRESS_LIST.add( AddressRange.makeNetwork( InetAddress.getByName( "169.254.0.0" ),
                                                                InetAddress.getByName( "255.255.0.0" ),
                                                                true ));
            
            /* Multicast */
            ILLEGAL_ADDRESS_LIST.add( AddressRange.makeNetwork( InetAddress.getByName( "224.0.0.0" ),
                                                                InetAddress.getByName( "240.0.0.0" ),
                                                                true ));

            /* Class E */
            ILLEGAL_ADDRESS_LIST.add( AddressRange.makeNetwork( InetAddress.getByName( "240.0.0.0" ),
                                                                InetAddress.getByName( "240.0.0.0" ),
                                                                true ));
        } catch ( Exception e ) {
            logger.error( "Unable to initialize illegal address list, using the empty list", e );
            ILLEGAL_ADDRESS_LIST.clear();
        }
    }
    
}
