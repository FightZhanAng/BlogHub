package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置。
 *
 * <p>之前这里调用了 {@code HttpsURLConnection.setDefaultSSLSocketFactory(...)} 和
 * {@code setDefaultHostnameVerifier(...)}：这是 JVM 全局设置，会把整个进程（包括
 * 调用 MiMo API 等第三方 HTTPS 服务）的证书校验全部关掉。现在只提供一个正常的
 * RestTemplate，不再修改全局 SSL 状态。</p>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}
