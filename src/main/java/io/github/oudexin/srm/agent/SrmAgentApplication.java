package io.github.oudexin.srm.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the synthetic SRM procurement collaboration portfolio project. */
@SpringBootApplication
@MapperScan("io.github.oudexin.srm.agent.business.srm.mapper")
public class SrmAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SrmAgentApplication.class, args);
    }
}
