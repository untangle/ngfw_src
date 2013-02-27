/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.json.JSONObject;
import org.apache.log4j.Logger;

/**
 * Convience class for dealing with LDAP-style Distinguished names (RFC 2253).  For
 * now, just wraps the Java APIs (which are a bit cryptic, but take care of encoding
 * issues).  It also treats everything as Strings, ignoring some of the details
 * of naming.
 * <br>
 * Note that instances are not threadsafe.
 *
 * DEVELOPER NOTES:
 *
 * I got lazy, and wrapped the Java APIs.  If
 * this is ever used in a performance-critical
 * app it should be re-written
 *
 *  - wrs 2005-01-12
 **/
@SuppressWarnings("serial")
public class DistinguishedName implements java.io.Serializable
{
    private static final Logger logger = Logger.getLogger( DistinguishedName.class );

    private JSONObject members;

    private DistinguishedName(JSONObject members)
    {
        this.members = members;
    }

    /**
     * Copy constructor
     */
    public DistinguishedName( DistinguishedName copy )
    {
        try {
            this.members = new JSONObject( copy.members, JSONObject.getNames( copy.members ) );
        } catch (Exception e) {
            logger.warn("Exception: ",e);
        }
    }


    /**
     * Generate a name given the ordered strings, as a convenience for GUI
     *
     * @param org the organization
     * @param orgUnit the organization unit
     * @param city the city
     * @param state the state
     * @param coutry the country
     *
     * @return the generated DistinguishedName
     *
     * @exception InvalidNameException if the type or value
     *            are in an unparsable format.
     */
    public DistinguishedName( String org, String orgUnit, String city, String state, String country ) 
    {
        members = new JSONObject();
        add("O", org);
        add("OU", orgUnit);
        add("L", city);
        add("ST", state);
        add("C", country);
    }

    /**
     * Get the value for the given type
     *
     * @param type the type
     *
     * @return the value, or null if there is no
     *         such type in this name
     */
    public String getValue(String type)
    {
        try {
            return (String) members.get( type );
        } catch (Exception e) {
            logger.warn("Exception: ",e);
            return null;
        }
    }

    /**
     * Add the given RDN
     *
     * @param type the type (e.g. "CN")
     * @param value the value (e.g. "www.yahoo.com").
     *
     *
     * @exception InvalidNameException if the type or value
     *            are in an unparsable format.
     */
    public void add( String type, String value )
    {
        try {
            new Rdn(type, value); //Will throw an exception
            members.put( type, value );
        } catch (Exception e) {
            logger.warn("Exception: ",e);
        }
    }

    /**
     * Convert the internal name/value pair members into
     * a proper DN string.  Note that the Java classes seem
     * to reorder some things (either because they are broken,
     * of they are doing us a service and enforcing some
     * ordering rules from the spec???).
     *
     * @return the String
     */
    public String toDistinguishedString() 
    {
        try {
            List<Rdn> rdns = new ArrayList<Rdn>();

            for( String key : JSONObject.getNames( members ) ) {
                rdns.add( new Rdn( key, members.get(key) ) );
            }
            return new LdapName(rdns).toString();
        }
        catch( Exception e ) {
            logger.warn("Exception: ", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * Enumerate all types (e.g. "OU") in the DN
     *
     * @return all types.
     */
    public List<String> listTypes()
    {
        String[] keys = JSONObject.getNames( this.members );
        return Arrays.asList( keys );
    }

    @Override
    public String toString()
    {
        return toDistinguishedString();
    }

    /**
     * Parse a String in XX=yyy,ZZ=acbd... format
     *
     * @param str the RFC 2253 string
     *
     * @return the parsed DistinguishedName
     *
     * @exception InvalidNameException if the string is an illegal Distinguished Name
     */
    public static DistinguishedName parse(String str) throws InvalidNameException
    {

        JSONObject ret = new JSONObject();

        LdapName ldapName = new LdapName(str.replaceAll(";","\\3B"));

        List<Rdn> rdns = ldapName.getRdns();

        try {
            for(Rdn rdn : rdns) {
                ret.put( rdn.getType().toString(), rdn.getValue().toString() );
            }
        } catch (Exception e) {
            logger.warn("Exception: ",e);
        }
        
        return new DistinguishedName(ret);
    }


    /**
     * Create a new (empty) Distinguished name)
     *
     * @return the new name
     */
    public static DistinguishedName create()
    {
        return new DistinguishedName( new JSONObject() );
    }


    public static void main(String[] args) throws Exception
    {
        //Create
        DistinguishedName newName = DistinguishedName.create();
        newName.add("L", "San, Mateo");
        newName.add("CN", "foo");
        newName.add("ST", "Cali");
        newName.add("OU", "Untangle");
        newName.add("X", "blaaa");

        System.out.println(newName.toDistinguishedString());

        //Parse
        DistinguishedName parseName = DistinguishedName.parse("L=San\\, Mateo, OU=Untangle, CN=foo, ST=Cali, X=blaaa");
        System.out.println("Second one's L = " + parseName.getValue("L"));
        System.out.println(parseName.toDistinguishedString());


    }

}

