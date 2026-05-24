package com.forever1996Fyk.ai.springai.mcpclient.ssl;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/24 21:24
 **/
public class HttpsMcpServer {
    public static void createInsecureHttpsClient(String baseUrl, String endpoint) {
        try {
            // 1. 创建一个信任所有证书的 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // 2. 初始化 SSL 上下文，绕过校验
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 3. 创建并配置 SSLParameters 以禁用主机名验证
            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm(null);

            // 4. 重新构建 httpClient
            HttpClient.Builder httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .sslContext(sslContext)
                    .sslParameters(sslParameters);

            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(baseUrl).sseEndpoint(endpoint)
                    .clientBuilder(httpClient)
                    .build();
            McpSyncClient mcp = McpClient.sync(transport).build();
            mcp.initialize();
        } catch (Exception e) {
            throw new RuntimeException("创建 Insecure MCP Client 失败", e);
        }
    }

    public static void createSecureHttpsClient(String baseUrl, String endpoint, String caCertPath) {
        try {
            // 1. 加载 CA 证书
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream fis = new FileInputStream(caCertPath);
            Certificate caCert = cf.generateCertificate(fis);
            fis.close();

            // 2. 创建 KeyStore 并导入 CA
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("caCert", caCert);

            // 3. 构建 TrustManagerFactory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            // 4. 创建 SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

            // 5. 使用默认 Hostname 验证
            HttpClient.Builder httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .sslContext(sslContext);

            // 6. 构建 SSE Transport
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(baseUrl)
                    .sseEndpoint(endpoint)
                    .clientBuilder(httpClient)
                    // 添加认证请求头
                    .requestBuilder(HttpRequest.newBuilder().header("Authorization", "Bearer abc123456789"))
                    .build();

            // 7. 初始化 MCP Client
            McpSyncClient mcp = McpClient.sync(transport).build();
            mcp.initialize();
            System.out.println("生产环境 MCP Client 初始化成功");

        } catch (Exception e) {
            throw new RuntimeException("创建 Secure MCP Client 失败", e);
        }
    }

    public static void main(String[] args) {
        createSecureHttpsClient("https://localhost:8443/",
                "/sse",
                "/Users/fanyukai/Desktop/develop/Idea Proejct/personal/AI-Learn/spring-ai/spring-ai-mcp/mcp-server-sse-https/src/main/resources/mcp-server.crt");
    }
}
