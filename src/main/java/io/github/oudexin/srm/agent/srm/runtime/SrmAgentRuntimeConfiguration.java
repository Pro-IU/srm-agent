package io.github.oudexin.srm.agent.srm.runtime;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SrmRuntimeProperties.class)
public class SrmAgentRuntimeConfiguration {
}
