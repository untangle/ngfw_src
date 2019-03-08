/**
 * $Id$
 */
package com.untangle.app.smtp;

/**
 * Enumeration of the SMTP Commands we know about (not that we accept all of them).
 */
public enum CommandType
{
    HELO("HELO"), EHLO("EHLO"), MAIL("MAIL"), RCPT("RCPT"), DATA("DATA"), RSET("RSET"), QUIT("QUIT"), SEND("SEND"), //
    SOML("SOML"), //
    SAML("SAML"), //
    TURN("TURN"), //
    VRFY("VRFY"),
    EXPN("EXPN"),
    HELP("HELP"),
    NOOP("NOOP"),
    SIZE("SIZE"),
    STARTTLS("STARTTLS"),
    AUTH("AUTH"),
    UNKNOWN("");

    private String code;

    /**
     * Initialize CommandType.
     * 
     * @param  code Code to initialie.
     * @return      Instance of CommandType
     */
    private CommandType(String code) {
        this.code = code;
    }

    /**
     * Get command type code.
     * @return String of code name.
     */
    public String getCode()
    {
        return code;
    }

    /**
     * Lookup code from string.
     * @param  code String of code to find.
     * @return      Matching type or UNKNOWN if not found.
     */
    public static CommandType fromCode(String code)
    {
        for (CommandType ct : CommandType.class.getEnumConstants()) {
            if (ct.getCode().equalsIgnoreCase(code)) {
                return ct;
            }
        }
        return UNKNOWN;
    }

};
