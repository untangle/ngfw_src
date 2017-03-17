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

    private AddressKind(char key, String kind) {
        this.key = key;
        this.kind = kind;
    }

    public static AddressKind getByKey(char key)
    {
        for (AddressKind ak : AddressKind.class.getEnumConstants()) {
            if (ak.getKey() == key) {
                return ak;
            }
        }
        return null;
    }

    public static AddressKind getByKind(String kindStr)
    {
        for (AddressKind ak : AddressKind.class.getEnumConstants()) {
            if (ak.getKind().equalsIgnoreCase(kindStr)) {
                return ak;
            }
        }
        return UNKNOWN;
    }

    public char getKey()
    {
        return key;
    }

    public String getKind()
    {
        return kind;
    }

    // Object kinds -----------------------------------------------------------

    public String toString()
    {
        return kind;
    }

    // Serialization ----------------------------------------------------------

    Object readResolve()
    {
        return getByKey(key);
    }
}
