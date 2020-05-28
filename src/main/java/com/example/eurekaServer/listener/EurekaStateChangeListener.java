package com.example.eurekaServer.listener;

import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.springframework.cloud.netflix.eureka.server.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.google.gson.Gson;

import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

/**
 * @Classname EurekaStateChangeListener
 * @Description 监听服务上下线
 * @Date 2019/9/3 13:22
 * @Created by gumei
 * @Author: lepua
 */
@Log4j2
@Component
public class EurekaStateChangeListener {
    @Value("${eureka.notify.http.url}")
    private String notifyUrl;

    @Value("${eureka.notify.http.async:true}")
    private Boolean asyncHttp;

    public static final org.apache.logging.log4j.Logger log =
            org.apache.logging.log4j.LogManager.getLogger(EurekaStateChangeListener.class);

    @EventListener
    public void listen(EurekaInstanceCanceledEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        httpSend(json);
        log.info("服务下线 " + json);
    }

    @EventListener
    public void listen(EurekaInstanceRegisteredEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        httpSend(json);
        log.info("服务注册 " + json);
    }

    @EventListener
    public void listen(EurekaInstanceRenewedEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        httpSend(json);
        log.info("服务续约 " + json);
    }

    @EventListener
    public void listen(EurekaRegistryAvailableEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        httpSend(json);
        log.info("注册中心启动 " + json);
    }

    @EventListener
    public void listen(EurekaServerStartedEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        httpSend(json);
        log.info("Eureka Server 启动 " + json);
    }

    // 发送通知
    @Async
    protected void httpSend(String msg) {
        StringEntity requestEntity = new StringEntity(
                msg,
                ContentType.APPLICATION_JSON);
        HttpPost request = new HttpPost(notifyUrl);
        request.setEntity(requestEntity);

        if (!asyncHttp) {
            // 同步调用发通知
            CloseableHttpClient httpClient = getHttpClient();
            try {
                HttpResponse rawResponse = httpClient.execute(request);
                log.debug(rawResponse);
            } catch (IOException e) {
                log.error(msg + " " + notifyUrl + "\n" + e.toString());
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        } else {
            // 异步调用(需要传入回调函数，不能重试)
            CloseableHttpAsyncClient httpClient = getAsyncHttpClient();
            //回调函数
            FutureCallback<HttpResponse> callback = new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    log.info(result.getStatusLine());
                }

                @Override
                public void failed(Exception e) {
                    log.error(e);
                }

                @Override
                public void cancelled() {
                    log.warn("cancelled");
                }
            };
            try {
                httpClient.start();
                Future<HttpResponse> future = httpClient.execute(request, callback);
                HttpResponse rawResponse = future.get();
                log.debug(rawResponse);
            } catch (InterruptedException e) {
                log.error(e);
            } catch (ExecutionException e) {
                log.error(e);
            } finally {
                try {
                    httpClient.close();
                } catch (IOException e) {
                   log.error(e);
                }
            }
        }
    }

    public static CloseableHttpAsyncClient getAsyncHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)//连接超时,连接建立时间,三次握手完成时间
                .setSocketTimeout(2000)//请求超时,数据传输过程中数据包之间间隔的最大时间
                .setConnectionRequestTimeout(20000)//使用连接池来管理连接,从连接池获取连接的超时时间
                .build();

        //配置io线程
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().
                setIoThreadCount(Runtime.getRuntime().availableProcessors())
                .setSoKeepAlive(true)
                .build();
        //设置连接池大小
        ConnectingIOReactor ioReactor = null;
        try {
            ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
        } catch (IOReactorException e) {
            log.error(e);
        }
        PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(ioReactor);
        // 将最大连接数增加到200
        cm.setMaxTotal(200);
        // 将每个路由基础的连接增加到20
        cm.setDefaultMaxPerRoute(20);
        return HttpAsyncClients.custom().
                setDefaultRequestConfig(requestConfig).
                setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE).
                setConnectionManager(cm).build();
    }


    public static CloseableHttpClient getHttpClient() {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        // 将最大连接数增加到200
        cm.setMaxTotal(200);
        // 将每个路由基础的连接增加到20
        cm.setDefaultMaxPerRoute(20);
        // 将特定主机的最大连接数增加到50
//        HttpHost localhost = new HttpHost("http://127.0.0.1",8080);
//        cm.setMaxPerRoute(new HttpRoute(localhost), 50);


        HttpRequestRetryHandler httpRequestRetryHandler = (exception, executionCount, context) -> {
            if (exception != null) {
                log.error("try request: " + executionCount + " " + exception.toString());
            }
            if (executionCount >= 5) {
                return false;
            }
            if (exception instanceof HttpHostConnectException) {// 如果连接失败，可能是服务在重启，等 5s 重试
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                       log.error(e);
                }
                return true;
            }
            if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                return true;
            }
            if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                return false;
            }
            if (exception instanceof InterruptedIOException) {// 超时
                return false;
            }
            if (exception instanceof UnknownHostException) {// 目标服务器不可达
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                return false;
            }
            if (exception instanceof SSLException) {// ssl握手异常
                return false;
            }

            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            // 如果请求是幂等的，就再次尝试
            if (!(request instanceof HttpEntityEnclosingRequest)) {
                return true;
            }
            return false;
        };

        int timeout = 3000;
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(timeout);
        requestBuilder.setConnectionRequestTimeout(timeout);

        return HttpClients.custom().setDefaultRequestConfig(requestBuilder.build())
                .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                .setConnectionManager(cm).setRetryHandler(httpRequestRetryHandler).build();
    }
}
