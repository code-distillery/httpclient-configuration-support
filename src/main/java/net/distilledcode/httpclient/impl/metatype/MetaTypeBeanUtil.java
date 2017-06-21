package net.distilledcode.httpclient.impl.metatype;

import net.distilledcode.httpclient.impl.metatype.reflection.GetterAdapter;
import net.distilledcode.httpclient.impl.metatype.reflection.Invokers;
import net.distilledcode.httpclient.impl.metatype.reflection.SetterAdapter;
import org.apache.commons.lang3.ClassUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MetaTypeBeanUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MetaTypeBeanUtil.class);

    private static final Map<Class<?>, Integer> ATTRIBUTE_TYPES;

    static {
        final HashMap<Class<?>, Integer> types = new HashMap<>();
        types.put(String.class, AttributeDefinition.STRING);
        types.put(Long.class, AttributeDefinition.LONG);
        types.put(Integer.class, AttributeDefinition.INTEGER);
        types.put(Short.class, AttributeDefinition.SHORT);
        types.put(Character.class, AttributeDefinition.CHARACTER);
        types.put(Byte.class, AttributeDefinition.BYTE);
        types.put(Double.class, AttributeDefinition.DOUBLE);
        types.put(Float.class, AttributeDefinition.FLOAT);
        types.put(Boolean.class, AttributeDefinition.BOOLEAN);
        ATTRIBUTE_TYPES = Collections.unmodifiableMap(types);
    }

    public static Map<String, Object> toMap(ServiceReference<?> serviceReference) {
        final Map<String, Object> map = new HashMap<>();
        for (final String key : serviceReference.getPropertyKeys()) {
            map.put(key, serviceReference.getProperty(key));
        }
        return map;
    }

    /**
     * For every key in the {@code configuration} map, the corresponding setter on
     * the {@code object} argument is called if it exists.
     *
     * @param namespace Namespace of the configuration, e.g. "request.config"
     * @param configuration Map containing the configuration to apply
     * @param object a SetterAdapter that allows setting the configuration values on the underlying object.
     */
    public static void applyConfiguration(final String namespace, final Map<String, Object> configuration, final SetterAdapter object) {
        final String prefix = normalizeNamespace(namespace);
        for (final Map.Entry<String, Object> entry : configuration.entrySet()) {
            final String prop = entry.getKey();
            if (prop.startsWith(prefix)) {
                final String propertyName = prop.replaceFirst(prefix, "");
                final Object value = entry.getValue();
                try {
                    object.set(propertyName, value);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOG.warn("Failed to set property '{}' to '{}'", propertyName, value, e);
                }
            }
        }
    }

    public static ObjectClassDefinition createObjectClassDefinition(final String id, final String name, final String description, final AttributeDefinition[] attributeDefinitions) {
        return new SimpleObjectClassDefinition(id, name, description, attributeDefinitions);
    }

    @SafeVarargs
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

    private static String dottedToTitle(final String dotted) {
        // IllegalArgumentException if there is an upper case character?
        final char[] chars = dotted.toCharArray();
        final StringBuilder result = new StringBuilder(chars.length - 1);
        for (int i = 0; i < chars.length; i++) {
            final char ch = chars[i];
            if (i == 0) {
                result.append(Character.toUpperCase(chars[i]));
            } else if (ch == '.') {
                if (i++ < chars.length) {
                    result.append(' ').append(Character.toUpperCase(chars[i]));
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static String camelToDotted(final String name) {
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

    public static AttributeDefinition[] attributeDefinitions(final String namespace, final Map<String, Invokers.Invoker<?>> setters) {
        return attributeDefinitions(namespace, setters, GetterAdapter.EMPTY);
    }

    public static <T> AttributeDefinition[] attributeDefinitions(final String namespace, final Map<String, Invokers.Invoker<?>> setters, final GetterAdapter defaultValues) {
        final ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        for (final Map.Entry<String, Invokers.Invoker<?>> setter : setters.entrySet()) {

            Invokers.Invoker<?> invoker = setter.getValue();
            Class<?>[] parameterTypes = invoker.getParameterTypes();

            if (parameterTypes.length != 1) {
                throw new IllegalStateException("Only methods with a single parameter should be available here " + Arrays.toString(parameterTypes));
            }

            final Class<?> argumentType = parameterTypes[0];
            Integer attributeType = getAttributeType(argumentType);
            if (attributeType == null) {
                continue;
            }

            String propertyName = setter.getKey();
            final String[] defaultValue = computeDefaultValue(propertyName, defaultValues);
            final AttributeDefinition attributeDefinition = new SimpleAttributeDefinition(
                    normalizeNamespace(namespace) + propertyName,
                    dottedToTitle(propertyName),
                    argumentType,
                    defaultValue
            );
            attributeDefinitions.add(attributeDefinition);
        }

        return attributeDefinitions.toArray(new AttributeDefinition[attributeDefinitions.size()]);
    }

    private static String normalizeNamespace(final String namespace) {
        return namespace.isEmpty() || namespace.endsWith(".") ? namespace : namespace + ".";
    }

    private static Integer getAttributeType(final Class<?> argumentType) {
        final Class<?> type = argumentType.isArray() ? argumentType.getComponentType() : argumentType;
        return ATTRIBUTE_TYPES.get(ClassUtils.primitiveToWrapper(type));
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

        @Override
        public String toString() {
            return "SimpleAttributeDefinition{" +
                    "id='" + getID() + '\'' +
                    ", name='" + getName() + '\'' +
                    ", cardinality=" + getCardinality() +
                    ", type=" + getType() +
                    ", defaultValue=" + Arrays.toString(defaultValue) +
                    '}';
        }
    }

    private static String[] computeDefaultValue(String propertyName, final GetterAdapter defaultValueObject) {
        try {
            final Object value = defaultValueObject.get(propertyName);
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
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOG.warn("Failed to get default value for {}", propertyName, e);
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
