package com.foodprocurement.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableConfigurationProperties(SecurityConfig.AppUsers.class)
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(AppUsers cfg, PasswordEncoder p) {
    if (cfg.users() == null || cfg.users().isEmpty()) {
      throw new IllegalStateException(
          "app.users 未配置任何账户，请在环境变量中设置 ADMIN_PASSWORD / BUYER_PASSWORD / SUPPLIER_PASSWORD");
    }
    var manager = new InMemoryUserDetailsManager();
    for (var a : cfg.users()) {
      if (a.username() == null || a.username().isBlank() || a.password() == null || a.password().isBlank()) {
        throw new IllegalStateException("账户缺少用户名或密码（请检查环境变量配置）");
      }
      String[] roles = (a.roles() == null || a.roles().isBlank()) ? new String[0] : a.roles().split(",");
      manager.createUser(User.withUsername(a.username()).password(p.encode(a.password())).roles(roles).build());
    }
    return manager;
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity h) throws Exception {
    return h.csrf(c -> c.disable())
        .authorizeHttpRequests(a -> a
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**").permitAll()
            .requestMatchers("/v1/me").authenticated()
            .requestMatchers("/v1/dashboard/**").hasAnyRole("ADMIN", "REGULATOR")
            .requestMatchers(HttpMethod.GET, "/v1/inquiries/**").hasAnyRole("ADMIN", "BUYER", "REGULATOR", "SUPPLIER")
            .requestMatchers("/v1/inquiries/*/bids").hasAnyRole("ADMIN", "BUYER", "SUPPLIER")
            .requestMatchers("/v1/**").hasAnyRole("ADMIN", "BUYER", "REGULATOR")
            .anyRequest().authenticated())
        .httpBasic(b -> {})
        .build();
  }

  @ConfigurationProperties(prefix = "app")
  public record AppUsers(List<Account> users) {
    public record Account(String username, String password, String roles) {}
  }
}
