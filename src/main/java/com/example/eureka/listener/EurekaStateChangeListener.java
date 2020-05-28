package com.example.eureka.listener;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.cloud.netflix.eureka.server.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.google.gson.Gson;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

import lombok.extern.log4j.Log4j2;

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

    public static final org.apache.logging.log4j.Logger log =
            org.apache.logging.log4j.LogManager.getLogger(EurekaStateChangeListener.class);

    @EventListener
    public void listen(EurekaInstanceCanceledEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
        log.info("服务下线 " + json);
    }

    @EventListener
    public void listen(EurekaInstanceRegisteredEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
        log.info("服务注册 " + json);
    }

    @EventListener
    public void listen(EurekaInstanceRenewedEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
        log.info("服务续约 " + json);
    }

    @EventListener
    public void listen(EurekaRegistryAvailableEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
        log.info("注册中心启动 " + json);
    }

    @EventListener
    public void listen(EurekaServerStartedEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
        log.info("Eureka Server 启动 " + json);
    }

    private void send(String msg) {
        StringEntity requestEntity = new StringEntity(
                msg,
                ContentType.APPLICATION_JSON);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost postMethod = new HttpPost(notifyUrl);
            postMethod.setEntity(requestEntity);
            HttpResponse rawResponse = httpClient.execute(postMethod);
        } catch (IOException e) {
            log.error(msg + " " + notifyUrl + "\n" + e.toString());
        }
    }
}
