package net.distilledcode.httpclient.impl.metatype;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetatypeBeanUtilTest {
    @Test
    public void toMap() throws Exception {
        final Map<String, Object> expectedMap = new HashMap<String, Object>();
        expectedMap.put("a", "1");
        expectedMap.put("b", 2);
        expectedMap.put("c", 3L);

        final ServiceReference<?> serviceReference = mock(ServiceReference.class);
        when(serviceReference.getPropertyKeys()).thenReturn(expectedMap.keySet().toArray(new String[expectedMap.size()]));
        when(serviceReference.getProperty(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                String argument = invocation.getArgumentAt(0, String.class);
                return expectedMap.get(argument);
            }
        });
        Map<String, Object> actualMap = MetatypeBeanUtil.toMap(serviceReference);
        assertThat(actualMap, equalTo(expectedMap));
    }

    @Test
    public void applyConfiguration() throws Exception {

        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("foo.bar", "The Foo Bar");
        properties.put("min.count", 5);
        properties.put("max.iteration.count", 10L);
        properties.put("maxIterationCount", 15L); // should be ignored
        properties.put("no.setter", "set it anyways");
        properties.put("type.mismatch", 10); // expects string
        properties.put("all.else", new String[]{ "foo", "bar"}); // expects string

        TestBean testBean = new TestBean();
        MetatypeBeanUtil.applyConfiguration(properties, testBean);

        TestBean defaultTestBean = new TestBean();
        assertThat(testBean.fooBar, equalTo(properties.get("foo.bar")));
        assertThat(testBean.minCount, equalTo(properties.get("min.count")));
        assertThat(testBean.maxIterationCount, equalTo(properties.get("max.iteration.count")));
        assertThat(testBean.typeMismatch, equalTo(defaultTestBean.typeMismatch));
        assertThat(testBean.noSetter, equalTo(defaultTestBean.noSetter));
        assertThat(testBean.allElse, equalTo(properties.get("all.else")));
    }

    @Test
    public void applyConfigurationWithNamespace() throws Exception {

        final String namespaceWithoutTrailingDot = "test.bean";
        final String namespace = namespaceWithoutTrailingDot + ".";

        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(namespace + "foo.bar", "The Foo Bar");
        properties.put("test-bean.min.count", 5); // typo on purpose
        properties.put(namespace + "max.iteration.count", 10L);
        properties.put("maxIterationCount", 15L); // should be ignored
        properties.put(namespace + "no.setter", "set it anyways");
        properties.put(namespace + "type.mismatch", 10); // expects string
        properties.put(namespace + "all.else", new String[]{ "foo", "bar"}); // expects string

        TestBean testBean = new TestBean();

        for (String ns : new String[]{ namespace, namespaceWithoutTrailingDot }) {

            MetatypeBeanUtil.applyConfiguration(ns, properties, testBean);

            TestBean defaultTestBean = new TestBean();
            assertThat(testBean.fooBar, equalTo(properties.get(namespace + "foo.bar")));
            assertThat(testBean.minCount, equalTo(defaultTestBean.minCount));
            assertThat(testBean.maxIterationCount, equalTo(properties.get(namespace + "max.iteration.count")));
            assertThat(testBean.typeMismatch, equalTo(defaultTestBean.typeMismatch));
            assertThat(testBean.noSetter, equalTo(defaultTestBean.noSetter));
            assertThat(testBean.allElse, equalTo(properties.get(namespace + "all.else")));
        }
    }

    @Test
    public void simpleAttributeDefinition() throws Exception {
        final AttributeDefinition[] attributeDefinitions = MetatypeBeanUtil.attributeDefinition(
                "the.id",
                "The Name",
                String.class,
                "The Default"
        );

        assertThat(attributeDefinitions.length, is(1));
        AttributeDefinition def = attributeDefinitions[0];
        assertThat(def.getID(), equalTo("the.id"));
        assertThat(def.getName(), equalTo("The Name"));
        assertThat(def.getType(), is(AttributeDefinition.STRING));
        assertThat(def.getCardinality(), is(0));
        assertThat(def.getDefaultValue(), equalTo(new String[] {"The Default"}));
        assertThat(def.getDescription(), equalTo("default: The Default"));
        assertThat(def.getOptionLabels(), nullValue());
        assertThat(def.getOptionValues(), nullValue());
    }

    @Test
    public void createAttributeDefinitionFromSetters() throws Exception {
        final AttributeDefinition[] attributeDefinitions =
                MetatypeBeanUtil.attributeDefinitionsFromSetters(TestBean.class, DefaultValues.INSTANCE);

        assertThat(attributeDefinitions.length, is(5));

        for (final AttributeDefinition attributeDefinition : attributeDefinitions) {
            assertThat(attributeDefinition, correspondsTo(
                    TestBean.EXPECTED_ATTRIBUTES.get(attributeDefinition.getID()),
                    false
            ));
        }
    }

    @Test
    public void createAttributeDefinitionFromSettersWithoutDefaults() throws Exception {
        final AttributeDefinition[] attributeDefinitions =
                MetatypeBeanUtil.attributeDefinitionsFromSetters(TestBean.class, null);

        assertThat(attributeDefinitions.length, is(5));

        for (final AttributeDefinition attributeDefinition : attributeDefinitions) {
            assertThat(attributeDefinition, correspondsTo(
                    TestBean.EXPECTED_ATTRIBUTES.get(attributeDefinition.getID()),
                    true
            ));
        }
    }

    @Test
    public void createAttributeDefinitionFromNonStandardSetters() throws Exception {
        final AttributeDefinition[] attributeDefinitions =
                MetatypeBeanUtil.attributeDefinitionsFromSetters(TestBean.class, DefaultValues.INSTANCE);

        assertThat(attributeDefinitions.length, is(5));

        for (final AttributeDefinition attributeDefinition : attributeDefinitions) {
            assertThat(attributeDefinition, correspondsTo(
                    TestBean.EXPECTED_ATTRIBUTES.get(attributeDefinition.getID()),
                    false
            ));
        }
    }

    @Test
    public void createAttributeDefinitionFromSettersWithNamespace() throws Exception {
        String namespace = "test.bean.";
        final AttributeDefinition[] attributeDefinitions =
                MetatypeBeanUtil.attributeDefinitionsFromSetters(namespace, TestBean.class, DefaultValues.INSTANCE);

        assertThat(attributeDefinitions.length, is(5));

        for (final AttributeDefinition attributeDefinition : attributeDefinitions) {
            assertThat(attributeDefinition, correspondsTo(
                    namespace,
                    TestBean.EXPECTED_ATTRIBUTES.get(attributeDefinition.getID().replaceFirst(namespace, "")),
                    false
            ));
        }
    }

    @Test
    public void createObjectClassDefinition() throws Exception {
        final ObjectClassDefinition objectClassDefinition = MetatypeBeanUtil.createObjectClassDefinition(
                TestBean.class.getName(),
                "Test Bean",
                "The configuration of the Test Bean",
                new AttributeDefinition[0]);
        assertThat(TestBean.class.getName(), equalTo(objectClassDefinition.getID()));
        assertThat("Test Bean", equalTo(objectClassDefinition.getName()));
        assertThat("The configuration of the Test Bean", equalTo(objectClassDefinition.getDescription()));
        assertThat(new AttributeDefinition[0], equalTo(objectClassDefinition.getAttributeDefinitions(ObjectClassDefinition.ALL)));
    }

    @Test
    public void join() throws Exception {
        String[] joined = MetatypeBeanUtil.join(new String[]{"foo", "bar"}, new String[0], new String[]{"foobar"});
        assertThat(new String[]{"foo", "bar", "foobar"}, equalTo(joined));
    }

    @SafeVarargs
    private static <T> AttributeDefinition attr(final String id, final String name, final Class<T> type, final T... defaultValues) {
        final String[] defaultStrings = new String[defaultValues.length];
        for (int i = 0; i < defaultValues.length; i++) {
            defaultStrings[i] = defaultValues[i].toString();
        }
        return MetatypeBeanUtil.attributeDefinition(id, name, type, defaultStrings)[0];
    }

    private Matcher<? super AttributeDefinition> correspondsTo(final AttributeDefinition expected, final boolean ignoreDefault) {
        return correspondsTo("", expected, ignoreDefault);
    }

    private Matcher<? super AttributeDefinition> correspondsTo(final String namespace, final AttributeDefinition expected, final boolean ignoreDefault) {
        return new BaseMatcher<AttributeDefinition>() {
            @Override
            public boolean matches(final Object item) {
                if (!(item instanceof AttributeDefinition)) {
                    return false;
                }

                if (expected == null) {
                    fail("Expected AttributeDefinition must not be null. This is an error in the test case.");
                }

                AttributeDefinition actual = (AttributeDefinition) item;
                return Objects.equals(namespace + expected.getID(), actual.getID()) &&
                        Objects.equals(expected.getName(), actual.getName()) &&
                        Objects.equals(expected.getCardinality(), actual.getCardinality()) &&
                        Objects.equals(expected.getType(), actual.getType()) &&
                        (ignoreDefault || Arrays.equals(expected.getDefaultValue(), actual.getDefaultValue()));
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText(expected.toString() +
                        " (" +
                        (namespace.isEmpty() ? "" : "namespace: " + namespace + ", ") +
                        "ignoreDefault: " + ignoreDefault +
                        ")");
            }
        };
    }

    private static class DefaultValues {

        static final DefaultValues INSTANCE = new DefaultValues();

        String getFooBar() {
            return "nothing much";
        }

        int getMinCount() {
            return 1;
        }

        long getMaxIterationCount() {
            return 3;
        }

        long getTypeMismatch() {
            return 42;
        }
    }

    private static class TestBean {

        private static final Map<String, AttributeDefinition> EXPECTED_ATTRIBUTES;
        static {
            EXPECTED_ATTRIBUTES = new HashMap<String, AttributeDefinition>();
            EXPECTED_ATTRIBUTES.put("foo.bar", attr("foo.bar", "Foo Bar", String.class, DefaultValues.INSTANCE.getFooBar()));
            EXPECTED_ATTRIBUTES.put("min.count", attr("min.count", "Min Count", Integer.class, DefaultValues.INSTANCE.getMinCount()));
            EXPECTED_ATTRIBUTES.put("max.iteration.count", attr("max.iteration.count", "Max Iteration Count", Long.class, DefaultValues.INSTANCE.getMaxIterationCount()));
            EXPECTED_ATTRIBUTES.put("type.mismatch", attr("type.mismatch", "Type Mismatch", String.class, Long.toString(DefaultValues.INSTANCE.getTypeMismatch())));
            EXPECTED_ATTRIBUTES.put("all.else", attr("all.else", "All Else", String[].class));
        }

        private String fooBar = "nothing much";

        private int minCount = 1;

        private long maxIterationCount = 3;

        private String typeMismatch = "never set";

        private String noSetter = "never set";
        
        private String[] allElse;

        public void setFooBar(final String fooBar) {
            this.fooBar = fooBar;
        }

        public void setMinCount(final int minCount) {
            this.minCount = minCount;
        }

        public void setMaxIterationCount(final long maxIterationCount) {
            this.maxIterationCount = maxIterationCount;
        }

        public void setTypeMismatch(final String typeMismatch) {
            this.typeMismatch = typeMismatch;
        }

        public void setAllElse(final String... allElse) {
            this.allElse = allElse;
        }
    }
}