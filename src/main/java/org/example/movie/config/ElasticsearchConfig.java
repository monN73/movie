package org.example.movie.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;


@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.uris}")
    private String esHost;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() {

        SSLContext sslContext;
        try {
            sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true) // 信任所有证书
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("无法创建跳过验证的 SSLContext", e);
        }

        RestClientBuilder builder = RestClient.builder(org.apache.http.HttpHost.create(esHost))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .setDefaultCredentialsProvider(credentialsProvider())
                );

        ElasticsearchTransport transport = new RestClientTransport(
                builder.build(),
                new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }

    private CredentialsProvider credentialsProvider() {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        return provider;
    }
}





