/**
 * $Id$
 */
package com.untangle.uvm.setup.jabsorb;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.callback.CallbackController;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeCheckValidator;

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
    public void preInvokeCallback( Object context, Object instance, Method method, Object[] arguments )
    {
        if (arguments == null) return;

        // Jabsorb hands us the *interface* Method (registerObject was called
        // with the interface class). Java does not inherit parameter annotations
        // from an impl method back to the interface method, so reading
        // @SafeCheckParam off `method` returns nothing. Resolve the matching
        // method on the actual impl class instead.
        Method annotated = method;
        try {
            annotated = instance.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException ignored) {
            // instance must implement the dispatched method; fall back to the
            // interface method (no annotations available).
        }
        Annotation[][] paramAnns = annotated.getParameterAnnotations(); 

        String mLabel = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        for (int i = 0; i < arguments.length; i++) {
            Object arg = arguments[i];
            if (i >= paramAnns.length) { SafeCheckValidator.validateAll(arg); continue; }
            if (arg instanceof String) {
                String s = (String) arg;
                for (Annotation a : paramAnns[i]) {
                    if (a instanceof SafeCheckParam) {
                        SafeCheckParam scp = (SafeCheckParam) a;
                        SafeCheckValidator.validate(s, scp.value(), scp.errorMessage(),
                            mLabel + "(arg " + i + ")");
                    }
                }
            } else {
                SafeCheckValidator.validateAll(arg);
            }
        }
    }

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
