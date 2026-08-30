package com.wms.engine;

import org.springframework.boot.SpringApplication;

public class TestWmsCoreEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(WmsCoreEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
