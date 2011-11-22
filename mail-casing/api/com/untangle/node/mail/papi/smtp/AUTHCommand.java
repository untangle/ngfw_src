/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mail.papi.smtp;

import com.untangle.node.token.ParseException;


/**
 * Class reprsenting an SMTP "AUTH" Command (RFC 2554)
 */
public class AUTHCommand
    extends Command {

    private String m_mechanismName;
    private String m_initialResponse;

    public AUTHCommand(String cmdStr,
                       String argStr) throws ParseException {
        super(CommandType.AUTH, cmdStr, argStr);
        parseArgStr();
    }

    /**
     * Get the name of the SASL mechanism.
     */
    public String getMechanismName() {
        return m_mechanismName;
    }

    /**
     * Note that the initial "response" (dumb name, but from the spec)
     * is still base64 encoded.
     */
    public String getInitialResponse() {
        return m_initialResponse;
    }

    @Override
    protected void setArgStr(String argStr) {
        super.setArgStr(argStr);
        parseArgStr();
    }

    private void parseArgStr() {
        String argStr = getArgString();
        if(argStr == null) {
            return;
        }
        argStr = argStr.trim();
        int spaceIndex = argStr.indexOf(' ');
        if(spaceIndex == -1) {
            m_mechanismName = argStr;
        }
        else {
            m_mechanismName = argStr.substring(0, spaceIndex);
            m_initialResponse = argStr.substring(spaceIndex+1, argStr.length());
        }
    }
}
