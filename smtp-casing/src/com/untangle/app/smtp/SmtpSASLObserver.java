/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.AsciiUtil.bbToString;
import static com.untangle.uvm.util.BufferUtil.findCrLf;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.untangle.app.smtp.NotAnSMTPResponseLineException;
import com.untangle.app.smtp.Response;
import com.untangle.app.smtp.ResponseParser;
import com.untangle.app.smtp.sasl.SASLObserver;
import com.untangle.uvm.util.ByteBufferBuilder;

/**
 * Class which acts to watch a SASL interaction, attempting to detect if the SASL exchange will result in a session with
 * a negotiated security layer (and thus we should enter passthru). <br>
 * <br>
 * This class <b>will</b> eat the final server response as-well, to prevent any synchronzation issues with classes
 * aligning requests/responses.
 */
class SmtpSASLObserver
{

    /**
     * Enum of outcomes for every time bytes are passed to instances of this class
     */
    enum SmtpSASLStatus
    {
        /**
         * Exchange still in progress (i.e. we do not know if it will be encrypted or not).
         */
        IN_PROGRESS,
        /**
         * The Observer (either through explicit SASL parsing of security layer negotiation or parsing confusion)
         * recommends that this session enter passthru mode.
         */
        RECOMMEND_PASSTHRU,
        /**
         * The exchange is complete, and did not result in the negotiation of a security layer.
         */
        EXCHANGE_COMPLETE
    };

    private final Logger m_logger = Logger.getLogger(SmtpSASLObserver.class);

    private final SASLObserver m_observer;
    private StringBuilder m_fromClientSB;
    private ByteBufferBuilder m_fromServerBBB = new ByteBufferBuilder();
    private final int m_maxEncodedLineSz;

    /**
     * Construct a SmtpSASLObserver based on the SASLObserver.
     * 
     * @param observer
     *            the SASLObserver
     */
    SmtpSASLObserver(SASLObserver observer) {
        m_observer = observer;
        m_maxEncodedLineSz = (observer.getMaxReasonableMessageSz() * 4) / 3;
    }

    /**
     * The initial client response (junk on the "AUTH XXXX..." line. May be an "=" which means "no initial response".
     * 
     * @param respStr
     *            the response String (may be null).
     * 
     * @return the status
     */
    SmtpSASLStatus initialClientResponse(String respStr)
    {
        if (respStr == null) {
            return SmtpSASLStatus.IN_PROGRESS;
        }

        respStr = respStr.trim();

        if ("".equals(respStr) || "=".equals(respStr)) {
            return SmtpSASLStatus.IN_PROGRESS;
        }

        byte[] bytes = base64Decode(respStr);

        if (bytes == null) {
            m_logger.warn("Nothing to Base64 decrypt.  Assume problem and go into PASSTHRU mode");
            return SmtpSASLStatus.RECOMMEND_PASSTHRU;
        }

        if (m_observer.initialClientData(ByteBuffer.wrap(bytes))) {
            return isKnownSecured() ? SmtpSASLStatus.RECOMMEND_PASSTHRU : SmtpSASLStatus.IN_PROGRESS;
        }
        return SmtpSASLStatus.IN_PROGRESS;
    }

    /**
     * After this method completes, the bytes consumed (i.e. the difference between the before/after positions of the
     * buffer) should be either (a) passed along to the other side (an unparser), or (b) passed along as some opaque
     * token (a parser). <br>
     * <br>
     * Bytes may remain in the buffer if the return is either RECOMMEND_PASSTHRU or EXCHANGE_COMPLETE. In the case of
     * RECOMMEND_PASSTHRU simply pass a copy of the original on. In the case of EXCHANGE_COMPLETE, pass along the
     * consumed bytes as opaque, then continue to parse the buffer as SMTP data.
     * 
     * @param buf
     *            the buffer of server data
     * 
     * @return the status
     */
    SmtpSASLStatus serverData(ByteBuffer buf)
    {
        // Look for the CRLF. If not found, we *buffer*
        // this line locally and wait for more data.
        while (buf.hasRemaining()) {
            int index = findCrLf(buf);
            ByteBuffer dup = buf.duplicate();
            dup.limit(index);
            boolean foundEOL = index != -1;
            if (foundEOL) {
                buf.position(index + 2);
                dup.limit(index + 2);
            } else {
                buf.position(buf.limit());
            }

            if ((m_fromServerBBB.size() + dup.remaining()) > m_maxEncodedLineSz) {
                m_logger.warn("SASL response message exceeded max size of " + m_maxEncodedLineSz);
                return SmtpSASLStatus.RECOMMEND_PASSTHRU;
            }

            m_fromServerBBB.add(dup);

            if (foundEOL) {
                ByteBuffer respBuffer = m_fromServerBBB.toByteBuffer();
                try {
                    Response resp = new ResponseParser().parse(respBuffer);
                    if (resp == null) {
                        // Odd
                        m_logger.warn("Multiline response during SASL is unexpected, but tolerated (\""
                                + bbToString(respBuffer) + "\")");
                        continue;
                    }
                    // Clear the buffer builder.
                    m_fromServerBBB.clear();

                    // Evaluate the response
                    if (resp.getCode() < 300) {
                        // We're done.
                        m_logger.debug("Ending SASL exchange with code \"" + resp.getCode()
                                + "\" with session known to be clear: " + isKnownClear());
                        return isKnownClear() ? SmtpSASLStatus.EXCHANGE_COMPLETE : SmtpSASLStatus.RECOMMEND_PASSTHRU;
                    } else if (resp.getCode() >= 400) {
                        // We're done
                        m_logger.debug("Ending SASL exchange with error code \"" + resp.getCode()
                                + "\". Assume session not encrypted");
                        return SmtpSASLStatus.EXCHANGE_COMPLETE;
                    } else {
                        m_logger.debug("Intermediate SMTP/SASL response code \"" + resp.getCode() + "\"");

                        // Now, we need the bytes from this response.
                        for (String s : resp.getArgs()) {
                            byte[] bytes = base64Decode(s);
                            if (m_observer.serverData(ByteBuffer.wrap(bytes))) {
                                if (isKnownSecured()) {
                                    m_logger.debug("Session will be secured.  Enter passthru");
                                    return SmtpSASLStatus.RECOMMEND_PASSTHRU;
                                }
                            }
                        }
                    }
                } catch (NotAnSMTPResponseLineException ex) {
                    m_logger.warn("Unable to parse SMTP/SASL response line \"" + bbToString(respBuffer) + "\"", ex);
                    return SmtpSASLStatus.RECOMMEND_PASSTHRU;
                }
            } else {
                // Nothing to do. Simply wait for more bytes
                return SmtpSASLStatus.IN_PROGRESS;
            }
        }
        return SmtpSASLStatus.IN_PROGRESS;
    }

    /**
     * After this method completes, the bytes consumed (i.e. the difference between the before/after positions of the
     * buffer) should be either (a) passed along to the other side (an unparser), or (b) passed along as some opaque
     * token (a parser). <br>
     * <br>
     * Bytes may remain in the buffer if the return is either RECOMMEND_PASSTHRU or EXCHANGE_COMPLETE. In the case of
     * RECOMMEND_PASSTHRU simply pass a copy of the original on. In the case of EXCHANGE_COMPLETE, pass along the
     * consumed bytes as opaque, then continue to parse the buffer as SMTP data.
     * 
     * @param buf
     *            the buffer of client data
     * 
     * @return the status
     */
    SmtpSASLStatus clientData(ByteBuffer buf)
    {
        // Look for the CRLF. If not found, we *buffer*
        // this line locally and wait for more data.
        //
        // Remember to look for the "*" indicating cancel.
        // Look for the CRLF. If not found, we *buffer*
        // this line locally and wait for more data.
        while (buf.hasRemaining()) {
            int index = findCrLf(buf);
            ByteBuffer dup = buf.duplicate();
            dup.limit(index);
            boolean foundEOL = index != -1;
            if (foundEOL) {
                buf.position(index + 2);
            } else {
                buf.position(buf.limit());
            }

            if (m_fromClientSB == null) {
                m_fromClientSB = new StringBuilder();
            }

            if (append(dup, m_fromClientSB)) {
                m_logger.warn("SASL message exceeded max size of " + m_maxEncodedLineSz);
                return SmtpSASLStatus.RECOMMEND_PASSTHRU;
            }

            if (foundEOL) {
                // Check for "*"
                String s = m_fromClientSB.toString().trim();
                m_fromClientSB = null;
                if ("*".equals(s)) {
                    m_logger.debug("Client has canceled SASL exchange.  Wait for "
                            + "server's response then end observation");
                    continue;
                }
                byte[] bytes = base64Decode(s);
                if (bytes == null) {
                    m_logger.warn("Null decoded client bytes.  Wait for server's response "
                            + "in case this is a sneaky client attempting to get us to bypass " + "filtering");
                } else {
                    if (m_observer.clientData(ByteBuffer.wrap(bytes))) {
                        if (isKnownSecured()) {
                            m_logger.debug("Base on reciept of client message and recomendation " + "of \""
                                    + m_observer.getMechanismName() + "\" SASL Observer, enter passthru");
                            return SmtpSASLStatus.RECOMMEND_PASSTHRU;
                        }
                    }
                }
            } else {
                // Nothing to do. Simply wait for more bytes
                return SmtpSASLStatus.IN_PROGRESS;
            }
        }
        return SmtpSASLStatus.IN_PROGRESS;
    }

    /**
     * This method consumes the buffer
     * @param buf ByteBuffer to append to.
     * @param sb StringBulder to add to buffer.
     * @return true means "this line is too damm long", otherwise ok.
     */
    private boolean append(ByteBuffer buf, StringBuilder sb)
    {
        if ((sb.length() + buf.remaining()) > m_maxEncodedLineSz) {
            return true;
        }
        while (buf.hasRemaining()) {
            sb.append((char) buf.get());
        }
        return false;
    }

    /**
     * Determine if this is known to be secured.
     * @return true if secured, otherwise false.
     */
    private boolean isKnownSecured()
    {
        return m_observer.exchangeUsingPrivacy() == SASLObserver.FeatureStatus.YES
                || m_observer.exchangeUsingIntegrity() == SASLObserver.FeatureStatus.YES;
    }

    /**
     * Determine if this is known to be nonsecured.
     * @return true if nonsecured, otherwise false.
     */
    private boolean isKnownClear()
    {
        return m_observer.exchangeUsingPrivacy() == SASLObserver.FeatureStatus.NO
                && m_observer.exchangeUsingIntegrity() == SASLObserver.FeatureStatus.NO;
    }

    /**
     * Decode string to a byte.
     * @param  s String to decode.
     * @return   decoded byte.
     */
    private byte[] base64Decode(String s)
    {
        if (s == null) {
            return null;
        }
        try {
            return Base64.decodeBase64(s.getBytes());
        } catch (Exception ex) {
            m_logger.warn(
                    "Exception base 64 decoding \"" + s + "\" for SASL mechanism \"" + m_observer.getMechanismName()
                            + "\" over SMTP", ex);
            return null;
        }
    }

}
