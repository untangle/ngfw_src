/**
 * $Id$
 */
package com.untangle.uvm.security;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.untangle.node.util.Pair;

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
 *  - wrs 1/12/05
 **/
@SuppressWarnings("serial")
public class DistinguishedName implements java.io.Serializable
{

    private List<Pair<String, String>> m_members;

    private DistinguishedName(List<Pair<String, String>> members) {
        m_members = members;
    }

    /**
     * Copy constructor
     */
    public DistinguishedName(DistinguishedName copy)
    {
        m_members = new ArrayList<Pair<String, String>>();
        for(Pair<String, String> pair : copy.m_members) {
            m_members.add(new Pair<String, String>(pair.a, pair.b));
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
    public DistinguishedName(String org, String orgUnit, String city, String state, String country) throws InvalidNameException
    {
        m_members = new ArrayList<Pair<String, String>>();
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
    public String getValue(String type) {
        int index = indexOf(type);
        return index==-1?
            null:
        m_members.get(index).b;
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
    public void add(String type, String value) throws InvalidNameException
    {
        new Rdn(type, value); //Will throw an exception
        m_members.add(new Pair<String, String>(type, value));
    }

    /**
     * Add the given RDN at the given index
     *
     * @param type the type (e.g. "CN")
     * @param value the value (e.g. "www.yahoo.com").
     * @param index the index
     *
     *
     * @exception InvalidNameException if the type or value
     *            are in an unparsable format.
     */
    public void add(String type, String value, int index) throws InvalidNameException
    {
        new Rdn(type, value);//Will throw an exception
        m_members.add(index, new Pair<String, String>(type, value));
    }

    /**
     * Get the index of the given type (case insensitive).
     *
     * @param type the type String (e.g. "CN").
     *
     * @return the index, or -1 if not found
     */
    public int indexOf(String type)
    {
        int index = 0;
        for(Pair<String, String> entry : m_members) {
            if(entry.a.equalsIgnoreCase(type)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Remove the name/value pair at the given index.  You
     * may also want to use {@link indexOf indexOf}.
     *
     * @param index the index
     */
    public void remove(int index)
    {
        m_members.remove(index);
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

            for(Pair<String, String> entry : m_members) {
                rdns.add(new Rdn(entry.a, entry.b));
            }
            return new LdapName(rdns).toString();
        }
        catch(InvalidNameException ex) {
            //This shouldn't happen but...
            throw new RuntimeException(ex);
        }
    }


    /**
     * Enumerate all types (e.g. "OU") in the DN
     *
     * @return all types.
     */
    public List<String> listTypes()
    {
        ArrayList<String> ret = new ArrayList<String>();
        for(Pair<String, String> entry : m_members) {
            ret.add(entry.a);
        }
        return ret;
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

        List<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();

        LdapName ldapName = new LdapName(str.replaceAll(";","\\3B"));

        List<Rdn> rdns = ldapName.getRdns();

        for(Rdn rdn : rdns) {
            ret.add(new Pair<String, String>(rdn.getType().toString(), rdn.getValue().toString()));
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
        return new DistinguishedName(new ArrayList<Pair<String, String>>());
    }


    public static void main(String[] args) throws Exception {

        //Create
        DistinguishedName newName = DistinguishedName.create();
        newName.add("L", "San, Mateo");
        newName.add("CN", "foo", 0);
        newName.add("ST", "Cali", 0);
        newName.add("OU", "Untangle");
        newName.add("X", "blaaa", 0);

        System.out.println(newName.toDistinguishedString());

        //Parse
        DistinguishedName parseName = DistinguishedName.parse("L=San\\, Mateo, OU=Untangle, CN=foo, ST=Cali, X=blaaa");
        System.out.println("Second one's L = " + parseName.getValue("L"));
        System.out.println(parseName.toDistinguishedString());


    }

}

