package com.untangle.app.smtp.handler;

import javax.mail.internet.MimeMessage;

import com.untangle.app.smtp.handler.SmtpTransactionHandler.BlockOrPassResult;

public class ScannedMessageResult
{
    private MimeMessage m_newMsg;
    private final BlockOrPassResult action;

    public ScannedMessageResult(BlockOrPassResult action)
    {
        this.action = action;
    }

    /**
     * Constructor used to create a result indicating that the message has been modified. This implicitly is not a block.
     */
    public ScannedMessageResult(MimeMessage newMsg)
    {
        action = BlockOrPassResult.PASS;
        m_newMsg = newMsg;
    }

    public boolean isBlock()
    {
        return action == BlockOrPassResult.DROP;
    }

    public BlockOrPassResult getAction()
    {
        return action;
    }

    public boolean messageModified()
    {
        return m_newMsg != null;
    }

    public MimeMessage getMessage()
    {
        return m_newMsg;
    }
}
