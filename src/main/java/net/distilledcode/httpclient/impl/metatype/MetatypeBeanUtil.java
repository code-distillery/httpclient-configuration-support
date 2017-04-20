package net.distilledcode.httpclient.impl.metatype;

import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MetatypeBeanUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MetatypeBeanUtil.class);


    private static final Map<Class<?>, Integer> ATTRIBUTE_TYPES;

    static {
        final HashMap<Class<?>, Integer> types = new HashMap<Class<?>, Integer>();
        types.put(String.class, AttributeDefinition.STRING);
        types.put(long.class, AttributeDefinition.LONG);
        types.put(Long.class, AttributeDefinition.LONG);
        types.put(int.class, AttributeDefinition.INTEGER);
        types.put(Integer.class, AttributeDefinition.INTEGER);
        types.put(short.class, AttributeDefinition.SHORT);
        types.put(Short.class, AttributeDefinition.SHORT);
        types.put(char.class, AttributeDefinition.CHARACTER);
        types.put(Character.class, AttributeDefinition.CHARACTER);
        types.put(byte.class, AttributeDefinition.BYTE);
        types.put(Byte.class, AttributeDefinition.BYTE);
        types.put(double.class, AttributeDefinition.DOUBLE);
        types.put(Double.class, AttributeDefinition.DOUBLE);
        types.put(float.class, AttributeDefinition.FLOAT);
        types.put(Float.class, AttributeDefinition.FLOAT);
        types.put(boolean.class, AttributeDefinition.BOOLEAN);
        types.put(Boolean.class, AttributeDefinition.BOOLEAN);
        ATTRIBUTE_TYPES = Collections.unmodifiableMap(types);
    }

    public static Map<String, Object> toMap(ServiceReference<?> serviceReference) {
        final Map<String, Object> map = new HashMap<String, Object>();
        for (final String key : serviceReference.getPropertyKeys()) {
            map.put(key, serviceReference.getProperty(key));
        }
        return map;
    }

    /**
     * For every key in the {@code configuration} map, the corresponding setter on
     * the {@code object} argument is called if it exists.
     *
     * @param configuration
     * @param object
     * @param <T>
     */
    public static <T> void applyConfiguration(final Map<String, Object> configuration, final T object) {
        for (final Map.Entry<String, Object> entry : configuration.entrySet()) {
            final String prop = entry.getKey();
            final Object value = entry.getValue();
            final String setterName = "set" + dottedToCamel(prop);
            final Class<?> valueClass = value.getClass();
            try {
                final Method setter = findMethod(object.getClass(), setterName, valueClass);
                if (setter != null) {
                    setter.invoke(object, value);
                }
            } catch (IllegalAccessException e) {
                LOG.warn("Not allowed to access method \"{}({})\"", setterName, valueClass, e);
            } catch (InvocationTargetException e) {
                LOG.warn("Failed to invoke method \"{}({})\"", setterName, valueClass, e);
            }
        }
    }

    public static ObjectClassDefinition createObjectClassDefinition(final String id, final String name, final String description, final AttributeDefinition[] attributeDefinitions) {
        return new SimpleObjectClassDefinition(id, name, description, attributeDefinitions);
    }

    public static <T> T[] join(final T[]... arrays) {
        int totalLength = 0;
        for (final T[] array : arrays) {
            totalLength += array.length;
        }

        if (arrays.length == 0) {
            throw new IllegalArgumentException("At least one argument must be provided.");
        }

        T[] flattenedArray = Arrays.copyOf(arrays[0], totalLength);

        int pos = arrays[0].length;
        for (int i = 1; i < arrays.length; i++) {
            T[] src = arrays[i];
            System.arraycopy(src, 0, flattenedArray, pos, src.length);
            pos += src.length;
        }
        return flattenedArray;
    }

    private static String camelToTitle(final String name) {
        final char[] chars = name.toCharArray();
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            final char ch = chars[i];
            if (Character.isUpperCase(ch) && i > 0) {
                result.append(' ');
            }
            result.append(ch);
        }
        return result.toString();
    }




    private static String camelToDotted(final String name) {
        // IllegalArgumentException if name contains a dot?
        final char[] chars = name.toCharArray();
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            final char ch = chars[i];
            if (Character.isUpperCase(ch)) {
                if (i != 0) {
                    result.append('.');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static <S, T> AttributeDefinition[] attributeDefinitionsFromSetters(Class<S> clazz, final T defaultValueObject) {
        final Method[] declaredMethods = clazz.getDeclaredMethods();
        final ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
        for (final Method declaredMethod : declaredMethods) {
            final String setterName = declaredMethod.getName();
            final int parameterCount = declaredMethod.getParameterTypes().length;
            if (!setterName.startsWith("set") || parameterCount != 1) {
                continue;
            }

            final Class<?> argumentType = declaredMethod.getParameterTypes()[0];

            Integer attributeType = getAttributeType(argumentType);
            if (attributeType == null) {
                continue;
            }

            final String camelName = setterName.substring(3);
            final String getterName = (attributeType == AttributeDefinition.BOOLEAN ? "is" : "get") + camelName;
            final String[] defaultValue = computeDefaultValue(defaultValueObject, getterName);

            final AttributeDefinition attributeDefinition = new SimpleAttributeDefinition(
                    camelToDotted(camelName),
                    camelToTitle(camelName),
                    argumentType,
                    defaultValue
            );
            attributeDefinitions.add(attributeDefinition);
        }

        return attributeDefinitions.toArray(new AttributeDefinition[attributeDefinitions.size()]);
    }

    private static Integer getAttributeType(final Class<?> argumentType) {
        final Class<?> type = argumentType.isArray() ? argumentType.getComponentType() : argumentType;
        return ATTRIBUTE_TYPES.get(type);
    }

    private static String dottedToCamel(final String name) {
        // IllegalArgumentException if there is an upper case character?
        final char[] chars = name.toCharArray();
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            final char ch = chars[i];
            if (i == 0) {
                result.append(Character.toUpperCase(chars[i]));
            } else if (ch == '.') {
                if (i++ < chars.length) {
                    result.append(Character.toUpperCase(chars[i]));
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private static Method findMethod(final Class<?> clazz, final String methodName, final Class<?>... expectedTypes) {
        methods: for (final Method method : clazz.getDeclaredMethods()) {
            final Class<?>[] actualTypes = method.getParameterTypes();
            if (method.getName().equals(methodName) && expectedTypes.length == actualTypes.length) {
                for (int i = 0; i < expectedTypes.length; i++) {
                    if (getAttributeType(expectedTypes[i]).intValue() != getAttributeType(actualTypes[i]).intValue()) {
                        continue methods;
                    }
                }
                return method;
            }
        }
        return null;
    }

    public static AttributeDefinition[] attributeDefinition(final String id, final String name, final Class<?> attributeType, final String... defaultValue) {
        return new AttributeDefinition[] {
                new SimpleAttributeDefinition(id, name, attributeType, defaultValue.length == 0 ? null : defaultValue)
        };
    }

    private static class SimpleAttributeDefinition implements AttributeDefinition {

        private final String id;
        private final String name;
        private final Class<?> attributeType;
        private final String[] defaultValue;

        public SimpleAttributeDefinition(final String id, final String name, final Class<?> attributeType, final String[] defaultValue) {
            if (getAttributeType(attributeType) == null) {
                throw new IllegalArgumentException("Unsupported attributeType: " + attributeType.getName());
            }
            this.id = id;
            this.name = name;
            this.attributeType = attributeType;
            this.defaultValue = defaultValue;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            final String[] defaultValue = getDefaultValue();
            if (defaultValue == null) {
                return "";
            }

            if (getCardinality() == 0 && defaultValue.length == 1) {
                return "default: " + getDefaultValue()[0];
            }
            return "default: " + Arrays.toString(getDefaultValue());
        }

        @Override
        public int getCardinality() {
            return attributeType.isArray() ? Integer.MAX_VALUE : 0;
        }

        @Override
        public int getType() {
            return getAttributeType(attributeType);
        }

        @Override
        public String[] getOptionLabels() {
            return null;
        }

        @Override
        public String[] getOptionValues() {
            return null;
        }

        @Override
        public String validate(final String value) {
            return null;
        }

        @Override
        public String[] getDefaultValue() {
            return defaultValue;
        }
    }

    private static String[] computeDefaultValue(final Object defaultValue, final String getterName) {
        if (defaultValue != null) {
            final Method method = findMethod(defaultValue.getClass(), getterName);
            if (method != null) {
                try {
                    final Object value = method.invoke(defaultValue);
                    if (value != null) {
                        if (value.getClass().isArray()) {
                            final Object[] values = (Object[]) value;
                            final String[] strings = new String[values.length];
                            for (int i = 0; i < values.length; i++) {
                                strings[i] = values[i].toString();
                            }
                            return strings;
                        } else {
                            return new String[]{value.toString()};
                        }
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Not allowed to access method \"{}()\"", getterName, e);
                } catch (InvocationTargetException e) {
                    LOG.warn("Failed to invoke method \"{}()\"", getterName, e);
                }
            }
        }
        return null;
    }

    private static class SimpleObjectClassDefinition implements ObjectClassDefinition {

        private final String id;
        private final String name;
        private final String description;
        private final AttributeDefinition[] attributeDefinitions;

        public SimpleObjectClassDefinition(final String id, final String name, final String description, AttributeDefinition[] attributeDefinitions) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.attributeDefinitions = attributeDefinitions;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public AttributeDefinition[] getAttributeDefinitions(final int filter) {
            return attributeDefinitions;
        }

        @Override
        public InputStream getIcon(final int size) throws IOException {
            return null;
        }
    }
}
