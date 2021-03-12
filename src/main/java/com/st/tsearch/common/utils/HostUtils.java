package com.st.tsearch.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Component
public class HostUtils implements ApplicationListener<WebServerInitializedEvent> {

    private static String ip;
    private static String hostName;
    private static int port;

    public static String getIp() {
        if (StringUtils.isBlank(ip)) {
            synchronized (HostUtils.class) {
                if (StringUtils.isBlank(ip)) {
                    initHostInfo();
                }
            }
        }
        return ip;
    }

    public static String getHostName() {
        if (StringUtils.isBlank(hostName)) {
            synchronized (HostUtils.class) {
                if (StringUtils.isBlank(hostName)) {
                    initHostInfo();
                }
            }
        }
        return hostName;
    }

    public static int getPort() {
        return port;
    }

    private static void initHostInfo() {
        InetAddress localHost = null;
        try {
            localHost = Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }

        if (ObjectUtils.isEmpty(localHost)) {
            return;
        }

        ip = localHost.getHostAddress();
        hostName = localHost.getHostName();
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
    }
}
