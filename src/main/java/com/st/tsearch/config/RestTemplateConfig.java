package com.st.tsearch.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Value("${http.maxTotal:100}")
    private int maxTotal;
    @Value("${http.defaultMaxPerRoute:20}")
    private int defaultMaxPerRoute;
    @Value("${http.connectTimeout:1000}")
    private int connectTimeout;
    @Value("${http.connectionRequestTimeout:1500}")
    private int connectionRequestTimeout;
    @Value("${http.socketTimeout:10000}")
    private int socketTimeout;
    @Value("${http.retryLimit:3}")
    private int retryLimit;
    @Value("${http.staleConnectionCheckEnabled:true}")
    private boolean staleConnectionCheckEnabled;

    @Bean
    public RestTemplate restTemplate(@Qualifier("httpComponentsClientHttpRequestFactory") HttpComponentsClientHttpRequestFactory factory) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(factory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        restTemplate.setMessageConverters(Arrays.asList(
                new StringHttpMessageConverter(StandardCharsets.UTF_8)
                , new ByteArrayHttpMessageConverter()
                , new ResourceHttpMessageConverter()
                , new FastJsonHttpMessageConverter()));
        log.info("init RestTemplate");
        return restTemplate;
    }

    @Bean(name = "httpComponentsClientHttpRequestFactory")
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory(@Qualifier("closeableHttpClient") CloseableHttpClient closeableHttpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(closeableHttpClient);
        factory.setReadTimeout(500_000);
        factory.setConnectTimeout(connectTimeout);
        factory.setConnectionRequestTimeout(connectionRequestTimeout);
        return factory;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    @Bean(name = "httpClientConnectionManager")
    public PoolingHttpClientConnectionManager getHttpClientConnectionManager() {
        PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager();
        //???????????????
        httpClientConnectionManager.setMaxTotal(maxTotal);
        //?????????
        httpClientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);

        httpClientConnectionManager.setValidateAfterInactivity(1);
        return httpClientConnectionManager;
    }

    /**
     * ????????????????????????????????????????????????
     * ?????????????????????????????????????????????????????????????????????
     *
     * @param httpClientConnectionManager
     * @return
     */
    @Bean(name = "httpClientBuilder")
    public HttpClientBuilder getHttpClientBuilder(@Qualifier("httpClientConnectionManager") PoolingHttpClientConnectionManager httpClientConnectionManager) {

        //HttpClientBuilder?????????????????????protected???????????????????????????????????????new??????????????????HttpClientBuilder???????????????HttpClientBuilder?????????????????????create()?????????HttpClientBuilder??????
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setConnectionManager(httpClientConnectionManager)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(retryLimit, true))
                .setKeepAliveStrategy((response, context) -> 200 * 1000) // ??????????????????????????????????????????Keep-Alive
                ;

        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build();
            httpClientBuilder.setSSLContext(sslContext);
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
            log.error("?????????HTTP??????????????? with root cause: {}", e.getMessage());
        }

        return httpClientBuilder;
    }

    /**
     * ??????????????????????????????httpClient
     *
     * @param httpClientBuilder
     * @return
     */
    @Bean(name = "closeableHttpClient")
    public CloseableHttpClient getCloseableHttpClient(@Qualifier("httpClientBuilder") HttpClientBuilder httpClientBuilder) {
        log.info("init CloseableHttpClient");
        return httpClientBuilder.build();
    }

    /**
     * Builder???RequestConfig??????????????????
     * ??????RequestConfig???custom????????????????????????Builder??????
     * ??????builder???????????????
     * ?????????????????????proxy???cookieSpec?????????????????????????????????????????????
     *
     * @return
     */
    @Bean(name = "builder")
    public RequestConfig.Builder getBuilder() {
        return RequestConfig.custom()
                .setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .setStaleConnectionCheckEnabled(staleConnectionCheckEnabled)
                ;
    }

    /**
     * ??????builder????????????RequestConfig??????
     *
     * @param builder
     * @return
     */
    @Bean
    public RequestConfig getRequestConfig(@Qualifier("builder") RequestConfig.Builder builder) {
        return builder.build();
    }
}

