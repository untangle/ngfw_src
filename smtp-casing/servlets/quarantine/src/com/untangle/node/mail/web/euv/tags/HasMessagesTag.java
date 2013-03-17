/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv.tags;

/**
 * Conditionaly includes page chunk if
 * messages are present
 */
@SuppressWarnings("serial")
public final class HasMessagesTag extends IfElseTag
{
    private String m_type = null;

    public void setType(String type)
    {
        m_type = type;
    }

    public String getType()
    {
        return m_type;
    }

    @Override
    public boolean isIncludeIfTrue()
    {
        return true;
    }

    @Override
    protected boolean isConditionTrue()
    {
        return QuarantineFunctions.hasMessages(pageContext.getRequest(), getType());
    }

}
