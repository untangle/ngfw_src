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
package com.untangle.node.smtp.sasl;
import static com.untangle.node.util.ASCIIUtil.bbToString;
import static com.untangle.node.util.ASCIIUtil.buffersEqual;
import static com.untangle.node.util.ASCIIUtil.isEOL;
import static com.untangle.node.util.ASCIIUtil.isLWS;

import java.nio.ByteBuffer;


/**
 * Implementation of the SASLObserver for the
 * "LOGIN" mechanism.
 * <br><br>
 * After extensive investigations, I'm 99% sure that
 * there is no RFC for this type of authentication.
 * <br><br>
 * General protocol *seems* to be as follows:
 * <br><br>
 * s: User Name null
 * c: my_user_name
 * s: Password null
 * c: my_password
 * <br><br>
 * There seems to be a null byte (0) at the end of each server
 * challenge, although I'm going to make it optional.  I've
 * also seen the variant "Username" instead of "User Name" for
 * the initial challenge.  Both are understood.
 * <br><br>
 * This will break if Client pipelines (sends UID/PWD before the server
 * prompts).  The alternative is to simply use the first complete
 * line from the client, but we risk (if things were out-or-order) printing
 * folks passwords into reports.
 */
class LOGINObserver
    extends ClearObserver {

    static final String[] MECH_NAMES = new String[] {
        "LOGIN".toLowerCase()
    };

    private static final ByteBuffer USERNAME_1 =
        ByteBuffer.wrap("username".getBytes());
    private static final ByteBuffer USERNAME_2 =
        ByteBuffer.wrap("username:".getBytes());
    private static final ByteBuffer USERNAME_3 =
        ByteBuffer.wrap("user name".getBytes());
    private static final ByteBuffer USERNAME_4 =
        ByteBuffer.wrap("user name:".getBytes());

    private String m_id;
    private boolean m_lastServerResponseUsername = false;


    LOGINObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }

    @Override
    public FeatureStatus exchangeAuthIDFound() {
        return m_id==null?
            FeatureStatus.UNKNOWN:FeatureStatus.YES;
    }

    @Override
    public String getAuthID() {
        return m_id;
    }

    @Override
    public boolean clientData(ByteBuffer buf) {

        if(m_lastServerResponseUsername) {

            //Trim trailing null (if found)
            if(
               buf.remaining() > 0 &&
               buf.get(buf.position() + buf.remaining()-1) == 0
               ) {
                if(buf.remaining() == 1) {
                    return false;
                }
                buf.limit(buf.limit()-1);
            }

            if(!buf.hasRemaining()) {
                return false;
            }
            m_id = bbToString(buf);
            return m_id != null;
        }
        return false;
    }

    @Override
    public boolean serverData(ByteBuffer buf) {

        fixupBuffer(buf);

        if(!buf.hasRemaining()) {
            return false;
        }

        //Compare buffer against our variants
        //of "User Name"
        m_lastServerResponseUsername = (buffersEqual(buf, USERNAME_1, true) ||
                                        buffersEqual(buf, USERNAME_2, true) ||
                                        buffersEqual(buf, USERNAME_3, true) ||
                                        buffersEqual(buf, USERNAME_4, true));
        return false;
    }

    private void fixupBuffer(ByteBuffer buf) {
        if(!buf.hasRemaining()) {
            return;
        }
        //Trim trailing null
        if(buf.get(buf.limit() - 1) == 0) {
            if(buf.remaining() == 1) {
                return;
            }
            buf.limit(buf.limit()-1);
        }

        //Trim leading/trailing LWS
        while(buf.hasRemaining()) {
            if(
               isEOL(buf.get(buf.position())) ||
               isLWS(buf.get(buf.position()))) {
                buf.get();
                continue;
            }
            break;
        }
        while(buf.hasRemaining()) {
            if(
               isEOL(buf.get(buf.limit()-1)) ||
               isLWS(buf.get(buf.limit()-1))) {
                buf.limit(buf.limit()-1);
                continue;
            }
            break;
        }
    }

}
