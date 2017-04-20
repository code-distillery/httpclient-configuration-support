# OSGi Configuration Support for Apache HttpComponents HttpClient Instances

This bundle provides the glue to inject pre-configured Apache HttpComponents HttpClient instances into OSGi services. The sample component below illustrates how a pre-configured HttpCLient instance is injected using Declarative Services annotations.

    @Component(service = TestService.class, immediate = true)
    public class TestService {
    
        private static final Logger LOG = LoggerFactory.getLogger(TestService.class);
    
        @Reference(target = "(http.client.id=test-client)")
        private HttpClient httpClient;
    
        @Activate
        private void activate() {
            LOG.info("Bound HttpClient {}", httpClient);
        }
    }

This service will only activate once a client with the id `test-client` is created by providing a configuration. In order to set a socket timeout of 5 seconds the following would work. 

    # file: org.apache.http.client.HttpClient-test-client.config
    http.client.id="test-client"
    request.config.socket.timeout=I"5000"

The implementation dynamically exposes all suitable setters (the ones with a single primitive type or String argument) of [`RequestConfig.Builder`](https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.Builder.html) and [`HttpClientBuilder`](https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/client/HttpClientBuilder.html) as configuration properties. 
