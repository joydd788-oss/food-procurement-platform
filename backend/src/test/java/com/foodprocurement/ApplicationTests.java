package com.foodprocurement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.users[0].username=admin",
    "app.users[0].password=admin123",
    "app.users[0].roles=ADMIN,REGULATOR",
    "app.users[1].username=buyer",
    "app.users[1].password=buyer123",
    "app.users[1].roles=BUYER",
    "app.users[2].username=supplier",
    "app.users[2].password=supplier123",
    "app.users[2].roles=SUPPLIER"
})
class ApplicationTests {
  @Test
  void contextLoads() {}
}
