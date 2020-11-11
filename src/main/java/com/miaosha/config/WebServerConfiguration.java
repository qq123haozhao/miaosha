package com.miaosha.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * @auhor: dhz
 * @date: 2020/5/10 16:45
 */

//当spring容器内没有TomcatEmbeddedServletContainerFactory这个bean时，会把此bean加载进tomcat容器

@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    //定制化内嵌tomcat
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        ((TomcatServletWebServerFactory)factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocol = (Http11NioProtocol)connector.getProtocolHandler();

                //30秒没有请求服务端自动断开keepAlive连接
                protocol.setKeepAliveTimeout(30000);

                //超过10000个请求自动断开keepAlive连接
                protocol.setMaxKeepAliveRequests(10000);

            }
        });
    }
}
