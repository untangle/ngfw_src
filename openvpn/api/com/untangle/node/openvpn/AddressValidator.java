/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.ValidationResult;

/** This validates a list of address/networks do not overlap.
 * It also validates that the networks do not overlap with the illegal/reserved
 * addresses listed in RFC 3330
 */
public class AddressValidator implements Validator
{
    private final Logger logger = Logger.getLogger(getClass());

    public static final String ERR_CODE_OVERLAP = "ERR_OVERLAP";
    
    private static final List<AddressRange> ILLEGAL_ADDRESS_LIST;
    private static final List<AddressRange> PRIVATE_ADDRESS_LIST;

    private static final AddressValidator INSTANCE = new AddressValidator();

    @SuppressWarnings("unchecked") //cast
    public ValidationResult validate(Object data) {

        try {
            if (data != null) {
                List<AddressRange> checkList = new LinkedList<AddressRange>( (List<AddressRange>) data );

                /* Make sure none of the addresses also overlap with the illegal ranges */
                checkList.addAll( ILLEGAL_ADDRESS_LIST );

                return checkOverlap( checkList );
            }
        } catch (Exception e) {
            return new ValidationResult(false, e.getMessage(), e);
        }

        return new ValidationResult(true);
    }
    
    public void validateOverlap( List<AddressRange> addressRangeList ) throws ValidateException
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>( addressRangeList );

        /* Make sure none of the addresses also overlap with the illegal ranges */
        checkList.addAll( ILLEGAL_ADDRESS_LIST );

        ValidationResult result = checkOverlap( checkList );

        if ( !result.isValid() )  {
            String[] data = (String[])result.getCause();
            throw new ValidateException("The two networks: " + data[0] +
                             " and " + data[1] + " cannot overlap" );
            
        }
    }

    public boolean isInPrivateNetwork( InetAddress address )
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>();

        /* Add an entry to the checklist for the single IP */
        checkList.add( AddressRange.makeAddress( address ));

        checkList.addAll( PRIVATE_ADDRESS_LIST );

        return checkOverlap( checkList ).isValid();
    }

    public boolean isIllegalAddress( InetAddress address )
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>();

        /* Add an entry to the checklist for the single IP */
        checkList.add( AddressRange.makeAddress( address ));

        checkList.addAll( ILLEGAL_ADDRESS_LIST );

        return checkOverlap( checkList ).isValid();
    }

    private ValidationResult checkOverlap( List<AddressRange> addressRangeList )
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>( addressRangeList );

        /* Sort the list */
        Collections.sort( checkList );

        /* Check for overlap */
        AddressRange previous = null;
        for ( AddressRange range : checkList ) {
            if ( previous != null ) {
                if (logger.isDebugEnabled()) {
                    logger.debug( "Checking address range: " + range.getDescription() +
                                  " against " + previous.getDescription());
                }

                if ( range.getStart() < previous.getEnd()) {
                    if ( range.getIsIllegal() && previous.getIsIllegal()) {
                        logger.warn( "Overlapping in the list of illegal addresses: " +
                                     range.getDescription() + "," + previous.getDescription());
                    } else if ( range.getIsIllegal()) {
                          return new ValidationResult(false,
                                  ERR_CODE_OVERLAP, new String[]{previous.getDescription(), range.getDescription()});
                    } else if ( previous.getIsIllegal()) {
                        return new ValidationResult(false,
                                ERR_CODE_OVERLAP, new String[]{range.getDescription(), previous.getDescription()});
                        
                        /* They are both legal */
                    } else {
                        return new ValidationResult(false,
                                ERR_CODE_OVERLAP, new String[]{range.getDescription(), previous.getDescription()});
                    }
                }
            }

            previous = range;
        }

        return new ValidationResult(true);
    }

    public static AddressValidator getInstance()
    {
        return INSTANCE;
    }

    static
    {
        List<AddressRange> illegalList = new LinkedList<AddressRange>();

        try {

            /* This list is from RFC 3330 */
            /* Loopback */
            illegalList.add( AddressRange.makeNetwork( InetAddress.getByName( "127.0.0.0" ),
                                                       InetAddress.getByName( "255.0.0.0" ),
                                                       true ));

            /* Link local (unassinged machines) */
            illegalList.add( AddressRange.makeNetwork( InetAddress.getByName( "169.254.0.0" ),
                                                       InetAddress.getByName( "255.255.0.0" ),
                                                       true ));

            /* Multicast */
            illegalList.add( AddressRange.makeNetwork( InetAddress.getByName( "224.0.0.0" ),
                                                       InetAddress.getByName( "240.0.0.0" ),
                                                       true ));

            /* Class E */
            illegalList.add( AddressRange.makeNetwork( InetAddress.getByName( "240.0.0.0" ),
                                                       InetAddress.getByName( "240.0.0.0" ),
                                                       true ));
        } catch ( Exception e ) {
            Logger logger = Logger.getLogger(AddressValidator.class);
            logger.error( "Unable to initialize illegal address list, using the empty list", e );
            illegalList.clear();
        }

        ILLEGAL_ADDRESS_LIST = Collections.unmodifiableList( illegalList );

        List<AddressRange> privateList = new LinkedList<AddressRange>();

        try {
            privateList.add( AddressRange.makeNetwork( InetAddress.getByName( "10.0.0.0" ),
                                                       InetAddress.getByName( "255.0.0.0" ),
                                                       false ));

            privateList.add( AddressRange.makeNetwork( InetAddress.getByName( "192.168.0.0" ),
                                                       InetAddress.getByName( "255.255.0.0" ),
                                                       false ));

            privateList.add( AddressRange.makeNetwork( InetAddress.getByName( "172.16.0.0" ),
                                                       InetAddress.getByName( "255.240.0.0" ),
                                                       false ));
        } catch ( Exception e ) {
            Logger logger = Logger.getLogger(AddressValidator.class);
            logger.error( "Unable to initialize illegal address list, using the empty list", e );
            privateList.clear();
        }

        PRIVATE_ADDRESS_LIST = Collections.unmodifiableList( privateList );
    }
}
