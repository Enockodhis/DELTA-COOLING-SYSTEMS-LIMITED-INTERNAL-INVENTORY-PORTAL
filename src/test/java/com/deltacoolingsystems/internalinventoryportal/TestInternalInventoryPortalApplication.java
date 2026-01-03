package com.deltacoolingsystems.internalinventoryportal;

import org.springframework.boot.SpringApplication;

public class TestInternalInventoryPortalApplication {

	public static void main(String[] args) {
		SpringApplication.from(InternalInventoryPortalApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
