/*
 *
 * Copyright (C) 1999-2016 IFLYTEK Inc.All Rights Reserved.
 * History：
 * Version   Author      Date                              Operation
 * 1.0       yaochen4    2016/8/11                           Create
 */
package com.iflytek;

import com.iflytek.config.Configuration;
import com.iflytek.config.Forward;
import org.apache.commons.configuration.ConfigurationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yaochen4
 * @version 1.0
 * @create 2016/8/11
 * @since 1.0
 */
@Component
public class ConfigUtils implements InitializingBean {

    @Value("classpath*:forward.xml")
    private Resource[] resources;
    @Value("file:/opt/forward/forward.xml")
    private Resource[] localResources;
    private Configuration configuration;
    private Map<String, Forward> map = new ConcurrentHashMap<>();


    public void getConfiguration() throws ConfigurationException, IOException {
        configuration = JAXB.unmarshal(resources[0].getURL(), Configuration.class);
        DirectoryWatcher watcher = new DirectoryWatcher(resources[0].getURL().toString());
        watcher.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                try {
                    synchronized (configuration) {
                        configuration = JAXB.unmarshal(resources[0].getURL(), Configuration.class);
                        initMap();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        watcher.execute();
        /*xmlConfiguration.setReloadingStrategy(new FileChangedReloadingStrategy());*/

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (localResources != null && localResources.length > 0 && localResources[0].exists()) {
            resources = localResources;
        }
        getConfiguration();
        initMap();
    }

    public void initMap() {
        map.clear();
        List<Forward> forwards = configuration.getForwards();
        for (Forward forward : forwards) {
            map.put(forward.getSource(), forward);
        }
    }

    public String getSource(String path) {
        String trimPath = trim(path);
        Set<String> keys = map.keySet();
        String hit = null;
        for (String key : keys) {
            String trimKey = trim(key);
            if (trimPath.matches(trimKey) || trimPath.startsWith(trimKey)) {
                hit = key;
                break;
            }
        }
        return hit;
    }

    public Forward getForward(String source) {
        if (source == null) {
            return null;
        }
        return map.get(source);
    }

    public String trim(String s) {
        String copy = s.trim();
        if (copy.startsWith("/")) {
            copy = copy.substring(1);
        }
        if (copy.endsWith("/")) {
            copy = copy.substring(0, copy.length() - 1);
        }
        return copy;
    }

}
