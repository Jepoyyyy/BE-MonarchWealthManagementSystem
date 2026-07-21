package com.indivaragroup.jdt17wms.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = SchedulingConfig.class)
class SchedulingConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void schedulingConfig_shouldBeLoadedIntoSpringContext() {
        SchedulingConfig config = applicationContext.getBean(SchedulingConfig.class);
        assertNotNull(config);
    }

}
