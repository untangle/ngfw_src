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

    /**
     * Initialize auth command.
     * 
     * @param  cmdStr String containing the command.
     * @param  argStr String containing argumnts.
     * @return        Instance of AuthCommand.
     */
    public AUTHCommand(String cmdStr, String argStr)
    {
        super(CommandType.AUTH, cmdStr, argStr);
        parseArgStr();
    }

    /**
     * Get the name of the SASL mechanism.
     *
     * @return Name of the SASL mechianism.
     */
    public String getMechanismName()
    {
        return m_mechanismName;
    }

    /**
     * Note that the initial "response" (dumb name, but from the spec) is still base64 encoded.
     *
     * @return Return base64 encoded reponse.
     */
    public String getInitialResponse()
    {
        return m_initialResponse;
    }

    /**
     * Parse the argument.
     * 
     * @param argStr Argument string to parse.
     */
    @Override
    protected void setArgStr(String argStr)
    {
        super.setArgStr(argStr);
        parseArgStr();
    }

    /**
     * Parse the argument string into mechanism and reponnse if space is found.
     */
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
