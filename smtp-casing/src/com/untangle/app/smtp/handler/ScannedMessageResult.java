/**
 * $Id$
 */
package com.untangle.app.smtp.handler;

import javax.mail.internet.MimeMessage;

import com.untangle.app.smtp.handler.SmtpTransactionHandler.BlockOrPassResult;

/**
 * Scanned message result
 */
public class ScannedMessageResult
{
    private MimeMessage m_newMsg;
    private final BlockOrPassResult action;

    /**
     * Initialize instance of ScannedMessageResult.
     * @param  action BlockOrPassResult to set.
     * @return        instance of ScannedMessageResult.
     */
    public ScannedMessageResult(BlockOrPassResult action)
    {
        this.action = action;
    }

    /**
     * Constructor used to create a result indicating that the message has been modified. This implicitly is not a block.
     * @param  newMsg MimeMessage to set.
     * @return        instance of ScannedMessageResult.
     */
    public ScannedMessageResult(MimeMessage newMsg)
    {
        action = BlockOrPassResult.PASS;
        m_newMsg = newMsg;
    }

    /**
     * Determine if message is blocked.
     * @return true if blocked.
     */
    public boolean isBlock()
    {
        return action == BlockOrPassResult.DROP;
    }

    /**
     * Return acton.
     * @return BlockOrPassResult.
     */
    public BlockOrPassResult getAction()
    {
        return action;
    }

    /**
     * Determine if message has been set.
     * @return true if message is non-null, false if null.
     */
    public boolean messageModified()
    {
        return m_newMsg != null;
    }

    /**
     * Return message.
     * @return MimeMessage of message.
     */
    public MimeMessage getMessage()
    {
        return m_newMsg;
    }
}
