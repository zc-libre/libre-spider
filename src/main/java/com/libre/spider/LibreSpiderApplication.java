package com.libre.spider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 小红书爬虫应用启动类
 *
 * @author libre
 */
@SpringBootApplication
@EnableScheduling
public class LibreSpiderApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibreSpiderApplication.class, args);
	}

}
