/**
 * $Id: UtCallbackController.java,v 1.00 2013/03/04 15:55:15 dmorris Exp $
 */
package com.untangle.uvm.setup.jabsorb;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.callback.CallbackController;

@SuppressWarnings("serial")
public class UtCallbackController extends CallbackController
{
    private final JSONRPCBridge bridge;

    UtCallbackController( JSONRPCBridge bridge )
    {
        this.bridge = bridge;
    }

    @Override
    public void preInvokeCallback( Object context, Object instance, Method method, Object[] arguments ) { }

    @Override
    public void postInvokeCallback( Object context, Object instance, Method method, Object result )
        throws Exception
    {
        if (result!= null && !(result instanceof Serializable)) {
            bridge.registerCallableReference(result.getClass());
        }
    }
}
