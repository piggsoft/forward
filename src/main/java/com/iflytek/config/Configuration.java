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
@XmlRootElement(name = "configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class Configuration {

    @XmlElementWrapper(name = "forwards")
    @XmlElement(name = "forward")
    private List<Forward> forwards;

    public List<Forward> getForwards() {
        return forwards;
    }

    public void setForwards(List<Forward> forwards) {
        this.forwards = forwards;
    }
}
