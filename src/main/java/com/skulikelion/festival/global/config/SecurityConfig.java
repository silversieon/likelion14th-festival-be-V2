package com.skulikelion.festival.global.config;

import com.skulikelion.festival.domain.booth.repository.BoothRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${swagger.auth.username}")
  private String swaggerUsername;

  @Value("${swagger.auth.password}")
  private String swaggerPassword;

  @Bean
  public UserDetailsService userDetailsService() {
    UserDetails swaggerUser =
        User.builder()
            .username(swaggerUsername)
            .password(passwordEncoder().encode(swaggerPassword))
            .roles("ADMIN")
            .build();

    return new InMemoryUserDetailsManager(swaggerUser);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // CSRF 보호 기능 비활성화 (REST API에서는 필요없음)
        .csrf(AbstractHttpConfigurer::disable)
        // HTTP Basic 인증 기본 설정
        .httpBasic(Customizer.withDefaults())
        // 세션 관리 설정
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        // CORS 설정 활성화 하지 않음. 서버에서 nginx로 CORS 검증
        .cors(AbstractHttpConfigurer::disable)
        // HTTP 요청에 대한 권한 설정
        .authorizeHttpRequests(
            request ->
                request
                    // 관리자 권한이 필요한 경로
                    .requestMatchers("/api/admin/**")
                    .authenticated()
                    // 개발자 권한이 필요한 경로(최종 배포 시 적용)
                    // .requestMatchers("/api/dev/**").authenticated()
                    // Swagger 경로 인증 필요
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .authenticated()
                    // 인증 없이 허용할 경로
                    .requestMatchers("/api/**")
                    .permitAll()
                    // 나머지는 모두 인증 필요
                    .anyRequest()
                    .authenticated());
    return http.build();
  }

  /** 비밀번호 인코더 Bean 등록 * */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 인증 관리자 Bean 등록 * */
  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}
