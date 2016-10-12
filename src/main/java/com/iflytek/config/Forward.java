/*
 *
 * Copyright (C) 1999-2016 IFLYTEK Inc.All Rights Reserved.
 * History：
 * Version   Author      Date                              Operation
 * 1.0       yaochen4    2016/10/11                           Create
 */
package com.iflytek.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

/**
 * @author yaochen4
 * @version 1.0
 * @create 2016/10/11
 * @since 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Forward {

    private String source;

    @XmlElementWrapper(name = "targets")
    @XmlElement(name = "target")
    private List<Target> targets;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }
}
