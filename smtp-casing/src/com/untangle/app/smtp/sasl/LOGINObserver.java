/**
 * $Id$
 */
package com.untangle.app.smtp.sasl;

import static com.untangle.uvm.util.AsciiUtil.bbToString;
import static com.untangle.uvm.util.AsciiUtil.buffersEqual;
import static com.untangle.uvm.util.AsciiUtil.isEOL;
import static com.untangle.uvm.util.AsciiUtil.isLWS;

import java.nio.ByteBuffer;

/**
 * Implementation of the SASLObserver for the "LOGIN" mechanism. <br>
 * <br>
 * After extensive investigations, I'm 99% sure that there is no RFC for this type of authentication. <br>
 * <br>
 * General protocol *seems* to be as follows: <br>
 * <br>
 * s: User Name null c: my_user_name s: Password null c: my_password <br>
 * <br>
 * There seems to be a null byte (0) at the end of each server challenge, although I'm going to make it optional. I've
 * also seen the variant "Username" instead of "User Name" for the initial challenge. Both are understood. <br>
 * <br>
 * This will break if Client pipelines (sends UID/PWD before the server prompts). The alternative is to simply use the
 * first complete line from the client, but we risk (if things were out-or-order) printing folks passwords into reports.
 */
class LOGINObserver extends ClearObserver
{

    static final String[] MECH_NAMES = new String[] { "LOGIN".toLowerCase() };

    private static final ByteBuffer USERNAME_1 = ByteBuffer.wrap("username".getBytes());
    private static final ByteBuffer USERNAME_2 = ByteBuffer.wrap("username:".getBytes());
    private static final ByteBuffer USERNAME_3 = ByteBuffer.wrap("user name".getBytes());
    private static final ByteBuffer USERNAME_4 = ByteBuffer.wrap("user name:".getBytes());

    private String m_id;
    private boolean m_lastServerResponseUsername = false;

    LOGINObserver() {
        super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
    }

    @Override
    public FeatureStatus exchangeAuthIDFound()
    {
        return m_id == null ? FeatureStatus.UNKNOWN : FeatureStatus.YES;
    }

    @Override
    public String getAuthID()
    {
        return m_id;
    }

    @Override
    public boolean clientData(ByteBuffer buf)
    {

        if (m_lastServerResponseUsername) {

            // Trim trailing null (if found)
            if (buf.remaining() > 0 && buf.get(buf.position() + buf.remaining() - 1) == 0) {
                if (buf.remaining() == 1) {
                    return false;
                }
                buf.limit(buf.limit() - 1);
            }

            if (!buf.hasRemaining()) {
                return false;
            }
            m_id = bbToString(buf);
            return m_id != null;
        }
        return false;
    }

    @Override
    public boolean serverData(ByteBuffer buf)
    {

        fixupBuffer(buf);

        if (!buf.hasRemaining()) {
            return false;
        }

        // Compare buffer against our variants
        // of "User Name"
        m_lastServerResponseUsername = (buffersEqual(buf, USERNAME_1, true) || buffersEqual(buf, USERNAME_2, true)
                || buffersEqual(buf, USERNAME_3, true) || buffersEqual(buf, USERNAME_4, true));
        return false;
    }

    private void fixupBuffer(ByteBuffer buf)
    {
        if (!buf.hasRemaining()) {
            return;
        }
        // Trim trailing null
        if (buf.get(buf.limit() - 1) == 0) {
            if (buf.remaining() == 1) {
                return;
            }
            buf.limit(buf.limit() - 1);
        }

        // Trim leading/trailing LWS
        while (buf.hasRemaining()) {
            if (isEOL(buf.get(buf.position())) || isLWS(buf.get(buf.position()))) {
                buf.get();
                continue;
            }
            break;
        }
        while (buf.hasRemaining()) {
            if (isEOL(buf.get(buf.limit() - 1)) || isLWS(buf.get(buf.limit() - 1))) {
                buf.limit(buf.limit() - 1);
                continue;
            }
            break;
        }
    }

}
