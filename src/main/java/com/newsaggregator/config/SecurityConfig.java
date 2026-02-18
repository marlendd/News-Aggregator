package com.newsaggregator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.newsaggregator.service.CaptchaService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private CaptchaService captchaService;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider(@Lazy UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @Lazy UserDetailsService userDetailsService, AuthenticationManager authenticationManager) throws Exception {
        
        // Создаем кастомный фильтр с проверкой капчи
        CaptchaAuthenticationFilter captchaFilter = new CaptchaAuthenticationFilter(captchaService);
        captchaFilter.setAuthenticationManager(authenticationManager);
        captchaFilter.setUsernameParameter("username");
        captchaFilter.setPasswordParameter("password");
        
        // Используем стандартный обработчик успеха, который сохраняет контекст безопасности
        org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler successHandler = 
            new org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/");
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        captchaFilter.setAuthenticationSuccessHandler(successHandler);
        
        captchaFilter.setAuthenticationFailureHandler((request, response, exception) -> {
            // Проверяем, была ли ошибка капчи
            Boolean captchaError = (Boolean) request.getSession().getAttribute("captchaError");
            if (captchaError != null && captchaError) {
                request.getSession().removeAttribute("captchaError");
                response.sendRedirect("/login?error=captcha");
            } else {
                response.sendRedirect("/login?error=credentials");
            }
        });
        
        http
            .authorizeHttpRequests(authz -> authz
                // Публичные страницы
                .requestMatchers("/", "/home", "/news", "/news/**", "/search", "/category/**", "/about", "/test", "/simple-test").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/register", "/error").permitAll()
                
                // API endpoints
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/user/**").hasAnyRole("USER", "EDITOR", "ADMIN")
                .requestMatchers("/api/editor/**").hasAnyRole("EDITOR", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Страницы администратора
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Страницы редактора
                .requestMatchers("/editor/**").hasAnyRole("EDITOR", "ADMIN")
                
                // Пользовательские страницы
                .requestMatchers("/user/**", "/profile/**", "/preferences/**").hasAnyRole("READER", "EDITOR", "ADMIN")
                
                // Все остальные запросы требуют аутентификации
                .anyRequest().authenticated()
            )
            .addFilterBefore(captchaFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .disable()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("uniqueAndSecret")
                .tokenValiditySeconds(86400) // 24 hours
                .userDetailsService(userDetailsService)
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            );
        
        return http.build();
    }
}