/*
 *
 * Copyright (C) 1999-2016 IFLYTEK Inc.All Rights Reserved.
 * History：
 * Version   Author      Date                              Operation
 * 1.0       yaochen4    2016/10/11                           Create
 */
package com.iflytek.config;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * @author yaochen4
 * @version 1.0
 * @create 2016/10/11
 * @since 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Target {
    private String url;
    @XmlAttribute
    private boolean isMain;
    @XmlElementWrapper(name = "headers")
    @XmlElement(name = "header")
    private List<Header> headers;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isMain() {
        return isMain;
    }

    public void setMain(boolean main) {
        isMain = main;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }
}
