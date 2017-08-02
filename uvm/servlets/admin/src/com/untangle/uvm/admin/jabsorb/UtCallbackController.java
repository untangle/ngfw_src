/**
 * $Id$
 */
package com.untangle.uvm.admin.jabsorb;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.callback.CallbackController;


@SuppressWarnings("serial")
public class UtCallbackController extends CallbackController
{
    private final JSONRPCBridge bridge;

    UtCallbackController(JSONRPCBridge bridge)
    {
        this.bridge = bridge;
    }

    @Override
    public void preInvokeCallback(Object context, Object instance, Method method, Object[] arguments) { }

    @Override
    public void postInvokeCallback(Object context, Object instance, Method method, Object result)  throws Exception
    {
        if (result!= null && !(result instanceof Serializable)) {
            bridge.registerCallableReference(result.getClass());
        }
    }
}