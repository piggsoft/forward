/*
 *
 * Copyright (C) 1999-2016 IFLYTEK Inc.All Rights Reserved.
 * History：
 * Version   Author      Date                              Operation
 * 1.0       yaochen4    2016/8/11                           Create
 */
package com.iflytek;

import com.iflytek.config.Forward;
import com.iflytek.config.Header;
import com.iflytek.config.Target;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * @author yaochen4
 * @version 1.0
 * @create 2016/8/11
 * @since 1.0
 */
@RestController
@RequestMapping("**")
public class CallbackController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackController.class);

    @Autowired
    private ConfigUtils configUtils;

    @Autowired
    private ThreadPoolTaskExecutor executor;

    private static final String DEFAULT_ENCODING = "UTF-8";

    @RequestMapping
    public void process(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        LOGGER.debug("requestURI: {}", path);
        String source = configUtils.getSource(path);
        if (source == null) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            try {
                response.getWriter().write("502, Bad gateway!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        Forward forward = configUtils.getForward(source);
        List<Target> targets = forward.getTargets();
        CloseableHttpClient httpClient = createClient(request);
        RequestBuilder builder = copyRequest(request);

        CloseableHttpResponse result = null;
        boolean hasMain = false;
        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);
            String realTarget = parseTarget(path, source, target.getUrl());
            addHeader(builder, target);
            builder.setUri(realTarget);
            HttpUriRequest httpUriRequest = builder.build();
            if (target.isMain()) {
                hasMain = true;
                result = send(realTarget, httpClient, httpUriRequest, true);
            } else if (!hasMain && i == targets.size() - 1) {
                result = send(realTarget, httpClient, httpUriRequest, true);
            } else {
                executor.execute(createTask(realTarget, httpClient, httpUriRequest));
            }
        }
        copyResponse(response, result);
    }

    public void copyResponse(HttpServletResponse response, CloseableHttpResponse result) {
        if (result == null) {
            return;
        }
        org.apache.http.Header[] headers = result.getAllHeaders();
        for (org.apache.http.Header header : headers) {
            if ("Transfer-Encoding".equalsIgnoreCase(header.getName())) {
                continue;
            }
            response.addHeader(header.getName(), header.getValue());
        }
        try {
            StreamUtils.copy(result.getEntity().getContent(), response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHeader(RequestBuilder forwardRequest, Target target) {
        List<Header> headers = target.getHeaders();
        if (headers != null && !headers.isEmpty()) {
            for (Header header : headers) {
                if ("host".equalsIgnoreCase(header.getName())) {
                    forwardRequest.removeHeaders(header.getName());
                    continue;
                }
                forwardRequest.removeHeaders(header.getName());
                forwardRequest.addHeader(header.getName(), header.getValue());
            }
        }
    }

    public String parseTarget(String path, String source, String target) {
        if (source.endsWith("/")) {
            return target;
        } else {
            return target + path.replaceFirst(source, "");
        }
    }

    public CloseableHttpResponse send(final String target, final CloseableHttpClient httpClient, final HttpUriRequest httpUriRequest, boolean isMain) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request to : {}, isMain : {}", target, isMain);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Request to : {}, isMain : {}", httpUriRequest.getRequestLine(), isMain);
        }
        try {
            CloseableHttpResponse response = httpClient.execute(httpUriRequest);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Response From {}, Response code: {}, isMain : {}", httpUriRequest.getRequestLine(), response.getStatusLine().toString(), isMain);
            }
            return response;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public Runnable createTask(final String target, final CloseableHttpClient httpClient, final HttpUriRequest httpUriRequest) {
        return new Runnable() {
            @Override
            public void run() {
                send(target, httpClient, httpUriRequest, false);
            }
        };
    }


    public RequestBuilder copyRequest(HttpServletRequest request) {
        String method = request.getMethod();

        RequestBuilder requestBuilder = parse(request);
        addHeader(request, requestBuilder);
        MultipartEntityBuilder builder = addBody(request, requestBuilder);
        if (builder != null) {
            addMultipartParams(request, requestBuilder, builder);
        } else {
            addParams(request, requestBuilder);
        }
        return requestBuilder;

    }

    public RequestBuilder parse(HttpServletRequest request) {
        String method = request.getMethod();
        RequestBuilder requestBuilder = null;
        if ("POST".equalsIgnoreCase(method)) {
            requestBuilder = RequestBuilder.post();
        } else if ("GET".equalsIgnoreCase(method)) {
            requestBuilder = RequestBuilder.get();
        } else if ("PUT".equalsIgnoreCase(method)) {
            requestBuilder = RequestBuilder.put();
        } else if ("DELETE".equalsIgnoreCase(method)) {
            requestBuilder = RequestBuilder.delete();
        } else if ("PATCH".equalsIgnoreCase(method)) {
            requestBuilder = RequestBuilder.patch();
        }
        requestBuilder.setCharset(Charset.forName(DEFAULT_ENCODING));
        return requestBuilder;
    }

    public void addHeader(HttpServletRequest request, RequestBuilder forwardRequest) {
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            final String header = headers.nextElement();
            if ("Content-Length".equalsIgnoreCase(header)) {
                continue;
            }
            if (header.startsWith("cookie")) {
                continue;
            }
            /*if ("host".equalsIgnoreCase(header)) {
                continue;
            }*/
            final Enumeration<String> values = request.getHeaders(header);
            while (values.hasMoreElements()) {
                final String value = values.nextElement();
                forwardRequest.addHeader(header, value);
            }
        }
    }

    public void addParams(HttpServletRequest request, RequestBuilder forwardRequest) {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            _addPostParams(request, forwardRequest);
        } else {
            _addParams(request, forwardRequest);

        }
    }

    private void _addParams(HttpServletRequest request, RequestBuilder forwardRequest) {
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            final String param = params.nextElement();
            String[] paramsValues = request.getParameterValues(param);
            for (String value : paramsValues) {
                LOGGER.debug("name: {}, value: {}", param, value);
                forwardRequest.addParameter(param, value);
            }
        }
    }

    private void _addPostParams(HttpServletRequest request, RequestBuilder forwardRequest) {
        List<NameValuePair> _params = new ArrayList<NameValuePair>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            final String param = params.nextElement();
            String[] paramsValues = request.getParameterValues(param);
            for (String value : paramsValues) {
                LOGGER.debug("name: {}, value: {}", param, value);
                _params.add(new BasicNameValuePair(param, value));
            }
        }
        try {
            if (_params.size() > 0) {
                forwardRequest.setEntity(new UrlEncodedFormEntity(_params, DEFAULT_ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        try {
            InputStream in = request.getInputStream();
            if (in != null) {
                byte[] bytes = StreamUtils.copyToByteArray(request.getInputStream());
                ContentType contentType = ContentType.create(request.getHeader("Content-Type"));
                ByteArrayEntity entity = new ByteArrayEntity(bytes, contentType);
                forwardRequest.setEntity(entity);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void addMultipartParams(HttpServletRequest request, RequestBuilder requestBuilder, MultipartEntityBuilder builder) {
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            final String param = params.nextElement();
            String[] paramsValues = request.getParameterValues(param);
            for (String value : paramsValues) {
                LOGGER.debug("name: {}, value: {}", param, value);
                builder.addTextBody(param, value, ContentType.TEXT_PLAIN.withCharset("UTF-8"));
            }
        }
        requestBuilder.setEntity(builder.build());
    }

    public MultipartEntityBuilder addBody(HttpServletRequest request, RequestBuilder forwardRequest) {
        MultipartEntityBuilder builder = null;
        try {
            //创建一个通用的多部分解析器
            CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
            //判断 request 是否有文件上传,即多部分请求
            if (multipartResolver.isMultipart(request)) {
                forwardRequest.removeHeaders("content-type");
                builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                builder.setCharset(Charset.forName("UTF-8"));
                //转换成多部分request
                MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
                //取得request中的所有文件名
                Iterator<String> iter = multiRequest.getFileNames();
                while (iter.hasNext()) {
                    //取得上传文件
                    MultipartFile file = multiRequest.getFile(iter.next());
                    if (file != null) {
                        ContentType contentType = ContentType.create(file.getContentType());
                        builder.addBinaryBody(file.getName(), file.getBytes(), contentType, file.getOriginalFilename());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder;
    }

    public CloseableHttpClient createClient(HttpServletRequest request) {
        /*Cookie[] cookies = request.getCookies();
        CookieStore cookieStore = new BasicCookieStore();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                BasicClientCookie forwardCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
                forwardCookie.setDomain("localhost");
                cookieStore.addCookie(forwardCookie);
            }
        }
        return HttpClients.custom().setDefaultCookieStore(cookieStore).build();*/
        return HttpClients.createDefault();
    }

}
