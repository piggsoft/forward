package com.iflytek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
public class ForwardApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForwardApplication.class, args);
	}

	@Bean
	public ThreadPoolTaskExecutor executor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(20);
		executor.setKeepAliveSeconds(30000);
		executor.setMaxPoolSize(1000);
		executor.setQueueCapacity(200);
		return executor;
	}
}
