package net.distilledcode.httpclient.impl;

import net.distilledcode.testing.osgi.ServiceUtils.Action;
import net.distilledcode.testing.osgi.ServiceUtils.ServiceAction;
import net.distilledcode.testing.osgi.ServiceUtils.ServicePropertiesAction;
import org.apache.http.client.HttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static net.distilledcode.httpclient.impl.HttpClientConfiguration.HTTP_CLIENT_CONFIG_NAME;
import static net.distilledcode.testing.osgi.PaxUtils.debug;
import static net.distilledcode.testing.osgi.PaxUtils.failOnUnresolvedBundle;
import static net.distilledcode.testing.osgi.PaxUtils.findProjectBundle;
import static net.distilledcode.testing.osgi.PaxUtils.logback;
import static net.distilledcode.testing.osgi.PaxUtils.webconsole;
import static net.distilledcode.testing.osgi.PaxUtils.workingDirectory;
import static net.distilledcode.testing.osgi.ServiceUtils.EVENT_TYPE_ANY;
import static net.distilledcode.testing.osgi.ServiceUtils.awaitServiceEvent;
import static net.distilledcode.testing.osgi.ServiceUtils.properties;
import static net.distilledcode.testing.osgi.ServiceUtils.withEachService;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.osgi.framework.Constants.SERVICE_RANKING;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HttpClientConfigurationIT {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientConfigurationIT.class);

    private static final String SOCKET_TIMEOUT = "request.config.socket.timeout";

    @Inject
    private BundleContext bundleContext;

    @Inject
    private HttpClient defaultHttpClient;

    @Inject
    private ConfigurationAdmin configurationAdmin;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() throws IOException, URISyntaxException {
        final File projectJarFile = findProjectBundle("httpclient-configuration-support-*.jar");
        return options(
                bootClasspathLibrary(toPath(org.ops4j.pax.url.link.Handler.class)), // support for link: protocol
                bootClasspathLibrary(toPath(org.ops4j.pax.url.classpath.Handler.class)), // support for classpath: protocol
                failOnUnresolvedBundle(),
                workingDirectory(getClass()),
                logback(),
                bundle(projectJarFile.toURI().toString()),
                linkBundle("org.apache.felix.configadmin"),
                linkBundle("org.apache.felix.eventadmin"),
                linkBundle("org.apache.felix.metatype"),
                linkBundle("org.apache.felix.scr"),
                linkBundle("org.apache.httpcomponents.httpclient"),
                linkBundle("org.apache.httpcomponents.httpcore"),
                linkBundle("org.apache.commons.lang"),

                // only required for the test setup
                junitBundles(),
                linkBundle("org.apache.commons.io"),
                cleanCaches(),
                debug(Integer.getInteger("debug.port", 0))
                        .useOptions(webconsole(Integer.getInteger("http.port", 8080)))
        );
    }

    private String toPath(final Class<?> clazz) throws URISyntaxException {
        return clazz.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
    }

    @Test
    public void defaultClientRegistration() throws Exception {
        assertThat(bundleContext, notNullValue());
        assertThat("Expected a HttpClient instance", defaultHttpClient, notNullValue());
    }

    @Test
    public void defaultClientRegistrationWithConfiguration() throws Exception {
        assertDefaultRegistration(HttpClient.class);
        assertDefaultRegistration(HttpClientBuilderFactory.class);
    }

    private <T> void assertDefaultRegistration(final Class<T> type) throws org.osgi.framework.InvalidSyntaxException, IOException {
        final String name = type.getSimpleName();
        String filter = "(!(service.pid=org.apache.http.httpclientfactory))";
        withEachService(bundleContext, type, filter, new ServicePropertiesAction() {
            @Override
            public void perform(final Map<String, Object> properties) {
                assertThat("Expected properties of a " + name + " instance", properties, notNullValue());
                assertThat(properties, not(hasKey(HTTP_CLIENT_CONFIG_NAME)));
                assertThat(properties, hasEntry(SERVICE_RANKING, (Object)100));
            }
        });

        final Configuration configuration = configFor(DefaultHttpClientConfiguration.DEFAULT_HTTP_CLIENT_CONFIG_PID);
        assertThat(configuration.getProperties(), nullValue());

        awaitServiceEvent(bundleContext, objectClassFilter(type), ServiceEvent.REGISTERED, new Action() {
            @Override
            public void perform() throws IOException {
                LOG.trace("Updating config {}", configuration);
                configuration.update(properties(SOCKET_TIMEOUT, 2000));
            }
        });

        withEachService(bundleContext, type, filter, new ServicePropertiesAction() {
            @Override
            public void perform(final Map<String, Object> properties) {
                assertThat("Expected properties of a " + name + " instance", properties, notNullValue());
                assertThat(properties, not(hasKey(HTTP_CLIENT_CONFIG_NAME)));
                assertThat(properties, hasEntry(SERVICE_RANKING, (Object)100));
                assertThat(properties, hasEntry(SOCKET_TIMEOUT, (Object)2000));
            }
        });

        awaitServiceEvent(bundleContext, objectClassFilter(HttpClient.class), ServiceEvent.REGISTERED, new Action() {
            @Override
            public void perform() throws IOException {
                try {
                    awaitServiceEvent(bundleContext, objectClassFilter(HttpClientBuilderFactory.class), ServiceEvent.REGISTERED, new Action() {
                        @Override
                        public void perform() throws IOException {
                            configuration.delete();
                        }
                    });
                } catch (InvalidSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        withEachService(bundleContext, type, filter, new ServiceAction<T>() {
            @Override
            public void perform(final T service) {
                assertThat("Default " + name + " should not require configuration", service, notNullValue());
            }
        });
    }

    @Test
    public void clientRegistrationWithFactoryConfiguration() throws Exception {
        assertFactoryConfigRegistration(HttpClient.class);
        assertFactoryConfigRegistration(HttpClientBuilderFactory.class);
    }

    private <T> void assertFactoryConfigRegistration(final Class<T> type) throws org.osgi.framework.InvalidSyntaxException, IOException {
        final String name = type.getSimpleName();
        withEachService(bundleContext, type, "(" + HTTP_CLIENT_CONFIG_NAME + "=test-client)", new ServiceAction<T>() {
            @Override
            public void perform(final T service) {
                assertThat(name + "(" + HTTP_CLIENT_CONFIG_NAME + "=test-client) should require configuration",
                        service, nullValue());
            }
        });

        final Configuration configuration = configurationAdmin.createFactoryConfiguration(HttpClientConfiguration.HTTP_CLIENT_CONFIG_FACTORY_PID, null);
        awaitServiceEvent(bundleContext, objectClassFilter(type), EVENT_TYPE_ANY, new Action() {
            @Override
            public void perform() throws IOException {
                LOG.trace("Updating config {}", configuration);
                configuration.update(properties(
                        HTTP_CLIENT_CONFIG_NAME, "test-client",
                        SOCKET_TIMEOUT, 5000
                ));
            }
        });

        withEachService(bundleContext, type, "(" + HTTP_CLIENT_CONFIG_NAME + "=test-client)", new ServicePropertiesAction() {
            @Override
            public void perform(final Map<String, Object> properties) {
                assertThat("Expected properties of a " + name + " instance", properties, notNullValue());
                assertThat(properties, hasEntry(HTTP_CLIENT_CONFIG_NAME, (Object)"test-client"));
                assertThat(properties, not(hasKey(SERVICE_RANKING)));
                assertThat(properties, hasEntry(SOCKET_TIMEOUT, (Object)5000));
            }
        });

        awaitServiceEvent(bundleContext, objectClassFilter(HttpClient.class), ServiceEvent.UNREGISTERING, new Action() {
            @Override
            public void perform() throws IOException {
                try {
                    awaitServiceEvent(bundleContext, objectClassFilter(HttpClientBuilderFactory.class), ServiceEvent.UNREGISTERING, new Action() {
                        @Override
                        public void perform() throws IOException {
                            configuration.delete();
                        }
                    });
                } catch (InvalidSyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        withEachService(bundleContext, type, "(" + HTTP_CLIENT_CONFIG_NAME + "=test-client)", new ServiceAction<T>() {
            @Override
            public void perform(final T service) {
                assertThat(name + "(" + HTTP_CLIENT_CONFIG_NAME + "=test-client) should require configuration",
                        service, nullValue());
            }
        });
    }

    private Configuration configFor(final String name) throws IOException {
        return configurationAdmin.getConfiguration(name, null);
    }

    private static String objectClassFilter(final Class<?> clazz) {
        return "(objectClass=" + clazz.getName() + ")";
    }

    private static <K, V> Matcher<? super Map<K, V>> hasEntry(final K key, final V value) {
        return new TypeSafeMatcher<Map<K,V>>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("has entry ");
                description.appendValue(key + "=" + value);
            }

            @Override
            protected boolean matchesSafely(final Map<K, V> map) {
                return map.containsKey(key) && map.get(key).equals(value);
            }
        };
    }

    private static <K, V> Matcher<? super Map<K, V>> hasKey(final K key) {
        return new TypeSafeMatcher<Map<K,V>>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("has key ");
                description.appendValue(key);
            }

            @Override
            protected boolean matchesSafely(final Map<K, V> map) {
                return map.containsKey(key);
            }
        };
    }
}
