package net.distilledcode.httpclient.impl.metatype.reflection;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

public class GetterAdapter extends AbstractInvokerAdapter {

    public static final GetterAdapter EMPTY = new GetterAdapter(null, Collections.<String, Invokers.Invoker<?>>emptyMap());

    public GetterAdapter(final Object delegate, final Map<String, Invokers.Invoker<?>> invokers) {
        super(delegate, invokers);
    }

    public Object get(final String invokerName) throws InvocationTargetException, IllegalAccessException {
        return invoke(invokerName);
    }
}
