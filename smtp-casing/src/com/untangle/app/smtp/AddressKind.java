/**
 * $Id$
 */
package com.untangle.app.smtp;

/**
 * Kind of Email address
 * 
 */
public enum AddressKind
{

    FROM('F', "FROM"), TO('T', "TO"), CC('C', "CC"),

    // These only apply to SMTP:
    ENVELOPE_FROM('G', "ENVELOPE_FROM"),
    ENVELOPE_TO('B', "ENVELOPE_TO"),

    // These only apply to IMAP/POP3:
    USER('U', "USER"),

    UNKNOWN('X', "");

    private final char key;
    private final String kind;

    // constructors -----------------------------------------------------------

    /**
     * Initialize type of address.
     * 
     * @param  key  Key of address type
     * @param  kind Description of address type.
     * @return      Instance of AddressKind
     */
    private AddressKind(char key, String kind) {
        this.key = key;
        this.kind = kind;
    }

    /**
     * Retreive address kind from key.
     * 
     * @param  key Address type to lookup.
     * @return     Matching AddressKind, null if if not found.
     */
    public static AddressKind getByKey(char key)
    {
        for (AddressKind ak : AddressKind.class.getEnumConstants()) {
            if (ak.getKey() == key) {
                return ak;
            }
        }
        return null;
    }

    /**
     * Retrieve address kind from string.
     * 
     * @param  kindStr String of address type.
     * @return     Matching AddressKind, null if if not found.
     */
    public static AddressKind getByKind(String kindStr)
    {
        for (AddressKind ak : AddressKind.class.getEnumConstants()) {
            if (ak.getKind().equalsIgnoreCase(kindStr)) {
                return ak;
            }
        }
        return UNKNOWN;
    }

    /**
     * For this instance, return the key.
     * 
     * @return Char key of kind.
     */
    public char getKey()
    {
        return key;
    }

    /**
     * For this instance, return the name.
     *
     * @return String of kind.
     */
    public String getKind()
    {
        return kind;
    }

    // Object kinds -----------------------------------------------------------

    /**
     * For this instance, return the name.
     *
     * @return String of kind.
     */
    public String toString()
    {
        return kind;
    }

    // Serialization ----------------------------------------------------------

    /**
     * For this instance, return the key.
     * 
     * @return Char key of kind.
     */
    Object readResolve()
    {
        return getByKey(key);
    }
}
