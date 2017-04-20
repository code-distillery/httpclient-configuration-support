package net.distilledcode.httpclient.impl.metatype;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
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

        Map<String, AttributeDefinition> attributeDefinitionsById = new HashMap<String, AttributeDefinition>();
        for (final AttributeDefinition attributeDefinition : attributeDefinitions) {
            attributeDefinitionsById.put(attributeDefinition.getID(), attributeDefinition);
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("foo.bar");
            assertThat(def.getID(), equalTo("foo.bar"));
            assertThat(def.getName(), equalTo("Foo Bar"));
            assertThat(def.getType(), is(AttributeDefinition.STRING));
            assertThat(def.getCardinality(), is(0));
            assertThat(def.getDefaultValue(), equalTo(new String[]{DefaultValues.INSTANCE.getFooBar()}));
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("min.count");
            assertThat(def.getID(), equalTo("min.count"));
            assertThat(def.getName(), equalTo("Min Count"));
            assertThat(def.getType(), is(AttributeDefinition.INTEGER));
            assertThat(def.getCardinality(), is(0));
            assertThat(def.getDefaultValue(), equalTo(new String[]{Integer.toString(DefaultValues.INSTANCE.getMinCount())}));
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("max.iteration.count");
            assertThat(def.getID(), equalTo("max.iteration.count"));
            assertThat(def.getName(), equalTo("Max Iteration Count"));
            assertThat(def.getType(), is(AttributeDefinition.LONG));
            assertThat(def.getCardinality(), is(0));
            assertThat(def.getDefaultValue(), equalTo(new String[]{Long.toString(DefaultValues.INSTANCE.getMaxIterationCount())}));
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("type.mismatch");
            assertThat(def.getID(), equalTo("type.mismatch"));
            assertThat(def.getName(), equalTo("Type Mismatch"));
            assertThat(def.getType(), is(AttributeDefinition.STRING));
            assertThat(def.getCardinality(), is(0));
            assertThat(def.getDefaultValue(), equalTo(new String[]{Long.toString(DefaultValues.INSTANCE.getTypeMismatch())}));
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("all.else");
            assertThat(def.getID(), equalTo("all.else"));
            assertThat(def.getName(), equalTo("All Else"));
            assertThat(def.getType(), is(AttributeDefinition.STRING));
            assertThat(def.getCardinality(), is(Integer.MAX_VALUE));
            assertThat(def.getDefaultValue(), nullValue());
        }
    }

    @Test
    public void createAttributeDefinitionFromSettersWithoutDefaults() throws Exception {
        final AttributeDefinition[] attributeDefinitions =
                MetatypeBeanUtil.attributeDefinitionsFromSetters(TestBean.class, null);

        Map<String, AttributeDefinition> attributeDefinitionsById = new HashMap<String, AttributeDefinition>();
        for (final AttributeDefinition attributeDefinition : attributeDefinitions) {
            attributeDefinitionsById.put(attributeDefinition.getID(), attributeDefinition);
        }

        assertThat(attributeDefinitions.length, is(5));

        {
            final AttributeDefinition def = attributeDefinitionsById.get("foo.bar");
            assertThat(def.getID(), equalTo("foo.bar"));
            assertThat(def.getName(), equalTo("Foo Bar"));
            assertThat(def.getType(), is(AttributeDefinition.STRING));
            assertThat(def.getDefaultValue(), nullValue());
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("min.count");
            assertThat(def.getID(), equalTo("min.count"));
            assertThat(def.getName(), equalTo("Min Count"));
            assertThat(def.getType(), is(AttributeDefinition.INTEGER));
            assertThat(def.getDefaultValue(), nullValue());
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("max.iteration.count");
            assertThat(def.getID(), equalTo("max.iteration.count"));
            assertThat(def.getName(), equalTo("Max Iteration Count"));
            assertThat(def.getType(), is(AttributeDefinition.LONG));
            assertThat(def.getDefaultValue(), nullValue());
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("type.mismatch");
            assertThat(def.getID(), equalTo("type.mismatch"));
            assertThat(def.getName(), equalTo("Type Mismatch"));
            assertThat(def.getType(), is(AttributeDefinition.STRING));
            assertThat(def.getDefaultValue(), nullValue());
        }

        {
            final AttributeDefinition def = attributeDefinitionsById.get("all.else");
            assertThat(def.getID(), equalTo("all.else"));
            assertThat(def.getName(), equalTo("All Else"));
            assertThat(def.getType(), is(AttributeDefinition.STRING));
            assertThat(def.getCardinality(), is(Integer.MAX_VALUE));
            assertThat(def.getDefaultValue(), nullValue());
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