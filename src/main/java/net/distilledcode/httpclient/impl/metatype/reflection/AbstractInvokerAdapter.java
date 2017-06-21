package net.distilledcode.httpclient.impl.metatype.reflection;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

abstract class AbstractInvokerAdapter {

    private final Object delegate;

    private final Map<String, Invokers.Invoker<?>> invokers;

    AbstractInvokerAdapter(Object delegate, Map<String, Invokers.Invoker<?>> invokers) {
        this.delegate = delegate;
        this.invokers = invokers;
    }

    Object invoke(final String invokerName, Object... params) throws InvocationTargetException, IllegalAccessException {
        Invokers.Invoker<?> invoker = invokers.get(invokerName);
        if (invoker != null && matchingParameterTypes(invoker.getParameterTypes(), params)) {
            return invoker.invoke(delegate, params);
        }
        return null;

    }

    private boolean matchingParameterTypes(final Class<?>[] parameterTypes, final Object[] params) {
        Class<?>[] normalizedParameterTypes = ClassUtils.primitivesToWrappers(parameterTypes);
        if (normalizedParameterTypes.length != params.length) {
            return false;
        }
        for (int i = 0; i < normalizedParameterTypes.length; i++) {
            if (!normalizedParameterTypes[i].isAssignableFrom(ClassUtils.primitiveToWrapper(params[i].getClass()))) {
                return false;
            }
        }
        return true;
    }

}
