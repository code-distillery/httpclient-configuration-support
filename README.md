# OSGi Configuration Support for Apache HttpComponents HttpClient Instances

The OSGi Configuration Support for Apache HttpComponents HttpClient Instances implements a set of Declarative Service components and MetaType definitions that allow multiple HttpClient instances to be configured via Configuration Admin and registered in the OSGi service registry, ready for other services to consume.

## Motivation

Furthermore, there is no default request timeout, which has caused production systems to become unavailable due to a single unresponsive backend system. It is a burden on developer productivity, if every service using an `HttpClient` needs to provide a configurable request timeout and possibly other configuration options. Rather than forcing developers to create ad-hoc configurations, a consistent approach to `HttpClient` configuration centralises the logic, can provide a sensible default configuration (e.g. with a short request timeout) and guarantees that these configuration options cannot be forgotten anymore.

The life-cycle of Apache HttpComponents HttpClient instance is not trivial to understand, and developers wanting to _just use_ an `HttpClient` often do not want to (and arguably should not need to) concern themselves with these aspects.

## Benefits
- editable default configuration for `HttpClient` instances
- editable non-default configuration for `HttpClient` instances
- possibility to assign non-default configurations by ID to a service (at development or runtime)
- automatic management of the life-cycle of `HttpClient` instances
- no more inconsistent ad-hoc configurations
- less work for developers using `HttpClient` in OSGi

## Requirements

- MetaType 1.3 (e.g. org.apache.felix.metatype 1.1.2)
- Declarative Services
- Configuration Admin

## How can I use it?

By default a configurable HttpClient is registered. This client will be injected into any field annotated with a `@Reference` (and no target filter), because its `service.ranking` is higher than the `service.ranking` of a HttpClient instance configured via a factory configuration.

To get started, the following is sufficient:

    @Component(service = TestService.class, immediate = true)
    public class TestService {
    
        private static final Logger LOG = LoggerFactory.getLogger(TestService.class);
    
        @Reference
        private HttpClient httpClient;
    
        @Activate
        private void activate() {
            LOG.info("Bound HttpClient {}", httpClient);
        }
    }

The __default HttpClient__ can be configured using the configuration-pid `org.apache.http.client.HttpClient.default`.

Example:

    # file: org.apache.http.client.HttpClient.default.config
    request.config.socket.timeout=I"2000"

In order to use a specific configuration fo `TestService`, a configuration can be created

    # file: TestService.config
    httpClient.target="(httpclient.config.name\=test-service)" # note the escaped "=" sign

This instructs `TestService` to require an `HttpClient` service with a matching property. By default this is not satisfied. However, a configuration can be provided:

    # file: org.apache.http.client.HttpClient-test-service.config
    httpclient.config.name="test-service"
    request.config.socket.timeout=I"5000"

It is also possible to define a target filter in the service's source code:

    @Component(service = TestService.class, immediate = true)
    public class TestService {

        private static final Logger LOG = LoggerFactory.getLogger(TestService.class);

        @Reference(target = "(httpclient.config.name=test-service)")
        private HttpClient httpClient;

        @Activate
        private void activate() {
            LOG.info("Bound HttpClient {}", httpClient);
        }
    }

The implementation dynamically exposes all suitable setters (the ones with a single primitive type or String argument) of [`RequestConfig.Builder`](https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.Builder.html) and [`HttpClientBuilder`](https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/client/HttpClientBuilder.html) as configuration properties. 
