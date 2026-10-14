package com.muzaffar.smartlead;

import org.springframework.boot.SpringApplication;

public class TestSmartLeadApplication {

    public static void main(String[] args) {
        SpringApplication.from(SmartLeadApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
