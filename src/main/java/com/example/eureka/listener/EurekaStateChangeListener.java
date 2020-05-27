package com.example.eureka.listener;

import com.netflix.appinfo.InstanceInfo;
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

/**
 * @Classname EurekaStateChangeListener
 * @Description 监听服务上下线
 * @Date 2019/9/3 13:22
 * @Created by gumei
 * @Author: lepua
 */
@Component
public class EurekaStateChangeListener {
    @Value("${eureka.notify.http.url}")
    private String notifyUrl;

    @EventListener
    public void listen(EurekaInstanceCanceledEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
//        System.out.println(eurekaInstanceCanceledEvent.getServerId() + "\t" + eurekaInstanceCanceledEvent.getAppName() + "服务下线");
    }

    @EventListener
    public void listen(EurekaInstanceRegisteredEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
//        InstanceInfo instanceInfo = event.getInstanceInfo();
//        System.err.println(event.getTimestamp() + " " + instanceInfo.getAppName() + " 进行注册 ");
    }

    @EventListener
    public void listen(EurekaInstanceRenewedEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
//        System.err.println(event.getTimestamp() + " " + event.getServerId() + "\t" + event.getAppName() + " 服务进行续约 ");
    }

    @EventListener
    public void listen(EurekaRegistryAvailableEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
//        System.err.println(event.getTimestamp() + " " + " 注册中心启动 ");
    }

    @EventListener
    public void listen(EurekaServerStartedEvent event) {
        Gson gson = new Gson();
        String json = gson.toJson(event);
        send(json);
//        System.err.println(event.getTimestamp() + " " + "Eureka Server 启动 ");
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
            System.err.println(msg + " " + notifyUrl + "\n" + e.toString());
        }
    }
}
