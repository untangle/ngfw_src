/**
 * $Id$
 */
package com.untangle.app.smtp;

/**
 * Class reprsenting an SMTP "AUTH" Command (RFC 2554)
 */
public class AUTHCommand extends Command
{

    private String m_mechanismName;
    private String m_initialResponse;

    public AUTHCommand(String cmdStr, String argStr)
    {
        super(CommandType.AUTH, cmdStr, argStr);
        parseArgStr();
    }

    /**
     * Get the name of the SASL mechanism.
     */
    public String getMechanismName()
    {
        return m_mechanismName;
    }

    /**
     * Note that the initial "response" (dumb name, but from the spec) is still base64 encoded.
     */
    public String getInitialResponse()
    {
        return m_initialResponse;
    }

    @Override
    protected void setArgStr(String argStr)
    {
        super.setArgStr(argStr);
        parseArgStr();
    }

    private void parseArgStr()
    {
        String argStr = getArgString();
        if (argStr == null) {
            return;
        }
        argStr = argStr.trim();
        int spaceIndex = argStr.indexOf(' ');
        if (spaceIndex == -1) {
            m_mechanismName = argStr;
        } else {
            m_mechanismName = argStr.substring(0, spaceIndex);
            m_initialResponse = argStr.substring(spaceIndex + 1, argStr.length());
        }
    }
}
