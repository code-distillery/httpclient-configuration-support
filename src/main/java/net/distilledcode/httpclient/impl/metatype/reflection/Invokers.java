package net.distilledcode.httpclient.impl.metatype.reflection;

import net.distilledcode.httpclient.impl.metatype.MetaTypeBeanUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Invokers {

    public static Map<String, Invoker<?>> beanGetters(final Object object) {
        return beanGetters(object.getClass());
    }

    public static Map<String, Invoker<?>> beanGetters(final Class<?> clazz) {
        return beanInvokers(clazz, 0, "get", "is");
    }

    public static Map<String, Invoker<?>> beanSetters(final Object object) {
        return beanSetters(object.getClass());
    }

    public static Map<String, Invoker<?>> beanSetters(final Class<?> clazz) {
        return beanInvokers(clazz, 1, "set");
    }

    private static Map<String, Invoker<?>> beanInvokers(final Class<?> type, final int parameterSize, final String... methodNamePrefixes) {
        final boolean isGetter = parameterSize == 0;
        final boolean isSetter = parameterSize == 1;

        if (!isGetter && !isSetter) {
            throw new IllegalArgumentException("parameterSize must be 0 or 1, it is " + parameterSize);
        }
        
        final Map<String, Invoker<?>> invokers = new HashMap<>();
        for (final Method method : type.getDeclaredMethods()) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            if (parameterSize == parameterTypes.length // 0 for getters, 1 for setters
                    && !(isGetter && returnType == Void.TYPE)) { // getters must not have a void return type
                final String name = method.getName();
                for (final String prefix : methodNamePrefixes) {
                    if (name.startsWith(prefix)) {
                        final String camelName = name.substring(prefix.length());
                        invokers.put(MetaTypeBeanUtil.camelToDotted(camelName), new Invoker<>(method));
                    }
                }
            }
        }
        return invokers;
    }

    public static Invoker<?> conditionalNoArgsSetter(final Method method, final boolean expectedParameter) {
        return new ConditionalInvoker(method, expectedParameter);
    }

    public static Invoker<?> defaultArgumentSetter(final Method method, final Object... defaultParameters) {
        return new DefaultParameterInvoker(method, defaultParameters);
    }

    /**
     * {@code Invoker} provide an indirection for calling {@code Method}s.
     * This allows e.g. injecting additional parameters in the "real" invocation,
     * or calling different methods depending on the parameter(s).
     *
     * @param <R> The type of the return value.
     */
    public static class Invoker<R> {

        private final Method method;
        
        public Invoker(Method method) {
            this.method = method;
        }

        /**
         * The expected parameter types.
         *
         * @return the expected parameter types
         */
        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        /**
         * Invoke a method on {@code object} with the {@code params}
         * provided. The params may be modified and/or interpreted
         * before the "real" method is invoked.
         *
         * @param object The object to invoke a method on.
         * @param params The parameters available for invoking the method.
         * @return The result of the invocation.
         */
        @SuppressWarnings("unchecked")
        public R invoke(final Object object, final Object... params)
                throws InvocationTargetException, IllegalAccessException {
            return (R) method.invoke(object, params);
        }
    }

    /**
     * The {@code ConditionalInvoker} calls a no args method if a single
     * boolean parameter is given and its value matched the {@code condition}.
     * <br>
     * This is useful for e.g. methods that enable or disable a feature.
     */
    private static class ConditionalInvoker extends Invoker<Void> {

        private final boolean condition;

        private ConditionalInvoker(final Method method, final boolean condition) {
            super(method);
            this.condition = condition;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class[]{ Boolean.class };
        }

        @Override
        public Void invoke(final Object object, final Object... params) throws InvocationTargetException, IllegalAccessException {
            if (params.length > 0 && params[0] instanceof Boolean && (boolean)params[0] == condition) {
                super.invoke(object);
            }
            return null;
        }
    }

    private static class DefaultParameterInvoker extends Invoker<Void> {
        private final Object[] defaultParameters;

        private final int paramIndex;

        public DefaultParameterInvoker(final Method method, final Object[] defaultParameters) {
            super(method);
            if (method.getParameterTypes().length != defaultParameters.length) {
                throw new IllegalArgumentException("defaultParameters needs to have as many entries as the method's parameter types '" + method.getName() + "'");
            }
            this.defaultParameters = defaultParameters;
            this.paramIndex = ArrayUtils.indexOf(defaultParameters, null);
        }

        @Override
        public Class<?>[] getParameterTypes() {
            if (paramIndex > -1) {
                Class<?> parameterType = super.getParameterTypes()[paramIndex];
                return new Class<?>[]{parameterType};
            } else {
                return new Class<?>[0];
            }
        }

        @Override
        public Void invoke(final Object object, final Object... params) throws InvocationTargetException, IllegalAccessException {
            if (paramIndex > -1) {
                if (params.length == getParameterTypes().length) {
                    Object[] newParams = Arrays.copyOf(defaultParameters, defaultParameters.length);
                    newParams[paramIndex] = params[0];
                    invoke(object, newParams);
                }
            }
            return null;
        }
    }
}
