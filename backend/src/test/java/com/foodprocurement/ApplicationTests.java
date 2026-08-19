package com.foodprocurement;
import org.junit.jupiter.api.Test; import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties={"spring.flyway.enabled=false","spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL","spring.jpa.hibernate.ddl-auto=create-drop"}) class ApplicationTests { @Test void contextLoads(){} }
