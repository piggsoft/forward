/*
 *
 * Copyright (C) 1999-2016 IFLYTEK Inc.All Rights Reserved.
 * History：
 * Version   Author      Date                              Operation
 * 1.0       yaochen4    2016/10/11                           Create
 */
package com.iflytek;

import com.iflytek.config.Configuration;
import com.iflytek.config.Forward;
import com.iflytek.config.Header;
import com.iflytek.config.Target;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import javax.xml.bind.JAXB;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yaochen4
 * @version 1.0
 * @create 2016/10/11
 * @since 1.0
 */
public class TestCase {

    @Test
    public void test01() {
        Configuration configuration = new Configuration();
        List<Forward> forwards = new ArrayList<>();
        Forward forward = new Forward();
        forward.setSource("asdasd");
        List<Target> targets = new ArrayList<>();
        Target target = new Target();
        target.setUrl("bbb");
        List<Header> headers = new ArrayList<>();
        Header header = new Header();
        header.setName("123");
        header.setValue("2323");
        headers.add(header);
        target.setHeaders(headers);
        targets.add(target);
        forward.setTargets(targets);
        forwards.add(forward);
        configuration.setForwards(forwards);
        JAXB.marshal(configuration, System.out);
    }

    public static void main(String[] args) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost("http://localhost:8080/v1/operator/simcard/460037641865880/active");

            StringBody comment = new StringBody("A binary file of some kind", ContentType.TEXT_PLAIN);

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("IDNO", comment)
                    .build();


            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                }
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
    }


}
