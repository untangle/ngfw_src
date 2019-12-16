/**
 * $Id: $
 */
package com.untangle.app.http;

import com.untangle.uvm.vnet.Token;

/**
 * 
 */
public class HttpRedirect
{
    public enum RedirectType {
        BLOCK,
        REDIRECT
    };
    private Token[] response;
    private RedirectType type =  RedirectType.BLOCK;

    /**
     * [HttpResponse description]
     * @param  response [description]
     * @param  type     [description]
     * @return          [description]
     */
    public HttpRedirect(Token[] response, RedirectType type)
    {
        this.response = response;
        this.type = type;
    }

    /**
     * [getResponse description]
     * @return [description]
     */
    public Token[] getResponse()
    {
        return this.response;
    }

    /**
     * [getType description]
     * @return [description]
     */
    public RedirectType getType()
    {
        return this.type;
    }

}