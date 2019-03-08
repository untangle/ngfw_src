/**
 * $Id$
 */
package com.untangle.uvm.setup.jabsorb;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.callback.CallbackController;

/**
 * UtCallbackController
 */
@SuppressWarnings("serial")
public class UtCallbackController extends CallbackController
{
    private final JSONRPCBridge bridge;

    /**
     * UtCallbackController
     * @param bridge
     */
    UtCallbackController( JSONRPCBridge bridge )
    {
        this.bridge = bridge;
    }

    /**
     * preInvokeCallback
     * @param context
     * @param instance
     * @param method
     * @param arguments
     */
    @Override
    public void preInvokeCallback( Object context, Object instance, Method method, Object[] arguments ) { }

    /**
     * postInvokeCallback
     * @param context
     * @param instance
     * @param method
     * @param result
     * @throws Exception
     */
    @Override
    public void postInvokeCallback( Object context, Object instance, Method method, Object result )
        throws Exception
    {
        if (result!= null && !(result instanceof Serializable)) {
            bridge.registerCallableReference(result.getClass());
        }
    }
}
