package net.distilledcode.httpclient.impl.metatype.reflection;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class SetterAdapter extends AbstractInvokerAdapter {

    public SetterAdapter(final Object delegate, final Map<String, Invokers.Invoker<?>> invokers) {
        super(delegate, invokers);
    }

    public void set(final String propertyName, Object value) throws InvocationTargetException, IllegalAccessException {
        invoke(propertyName, value);
    }
}
