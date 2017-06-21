package net.distilledcode.httpclient.impl.metatype.reflection;

import org.apache.http.client.config.RequestConfig;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class InvokersTest {

    private static class TestBean {

        private boolean featureEnabled = true;

        // getters
        public String getFooBar() { return null; }
        public void getFooBarVoid() {}

        // setters
        public void setBarFoo(String fooBar) {}
        public void setBarFooNoArgs() {}

        // boolean switch (only called for enabling, disabled by default
        public void enableFeature() {
            featureEnabled = true;
        }
        
        // boolean switch (only called for disabling, enabled by default
        void disableFeature() {
            featureEnabled = false;
        }
    }

    @Test
    public void invokeMethods() throws Exception {
        // builder.setMaxRedirects(5)
        Invokers.Invoker<Void> setMaxRedirects = new Invokers.Invoker<>(RequestConfig.Builder.class.getDeclaredMethod("setMaxRedirects", int.class));
        RequestConfig.Builder builder = RequestConfig.custom();
        setMaxRedirects.invoke(builder, 17);

        // requestConfig.getMaxRedirects()
        Invokers.Invoker<Integer> getMaxRedirects = new Invokers.Invoker<>(RequestConfig.class.getDeclaredMethod("getMaxRedirects"));
        RequestConfig requestConfig = builder.build();
        assertThat(getMaxRedirects.invoke(requestConfig), is(17));
    }

    @Test
    public void beanGetters() throws Exception {
        Map<String, Invokers.Invoker<?>> testBeanGetters = Invokers.beanGetters(TestBean.class);
        assertThat(testBeanGetters.keySet(), allOf(
                hasItem("foo.bar"),
                not(hasItem("foo.bar.void"))
        ));
    }

    @Test
    public void beanSetters() throws Exception {
        Map<String, Invokers.Invoker<?>> testBeanGetters = Invokers.beanSetters(TestBean.class);
        assertThat(testBeanGetters.keySet(), allOf(
                hasItem("bar.foo"),
                not(hasItem("bar.foo.no.args"))
        ));
    }

    @Test
    public void conditionalSetter() throws Exception {
        Invokers.Invoker<?> featureDisabler = Invokers.conditionalNoArgsSetter(TestBean.class.getDeclaredMethod("disableFeature"), false);

        TestBean testBean = new TestBean();
        assertThat(testBean.featureEnabled, is(true));
        featureDisabler.invoke(testBean, false);
        assertThat(testBean.featureEnabled, is(false));
    }

    @Test
    public void conditionalSetterIgnored() throws Exception {
        Invokers.Invoker<?> featureDisabler = Invokers.conditionalNoArgsSetter(TestBean.class.getDeclaredMethod("disableFeature"), true);
        
        TestBean testBean = new TestBean();
        assertThat(testBean.featureEnabled, is(true));
        featureDisabler.invoke(testBean, false);
        assertThat(testBean.featureEnabled, is(true));
    }
}
