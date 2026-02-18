package com.newsaggregator.config;

import java.io.IOException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.newsaggregator.service.CaptchaService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Фильтр для проверки капчи при входе в систему
 */
public class CaptchaAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    
    private final CaptchaService captchaService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    
    public CaptchaAuthenticationFilter(CaptchaService captchaService) {
        this.captchaService = captchaService;
        // Устанавливаем URL для обработки только POST-запросов
        setRequiresAuthenticationRequestMatcher(
            new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/login", "POST")
        );
    }
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        
        // Проверяем капчу перед попыткой аутентификации
        String captchaAnswer = request.getParameter("captcha");
        
        if (captchaAnswer == null || !captchaService.validateCaptcha(captchaAnswer)) {
            // Сохраняем специальный флаг для ошибки капчи
            request.getSession().setAttribute("captchaError", true);
            throw new BadCredentialsException("CAPTCHA_ERROR");
        }
        
        // Если капча верна, продолжаем стандартную аутентификацию
        return super.attemptAuthentication(request, response);
    }
    
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, Authentication authResult) throws IOException, ServletException {
        
        // Сохраняем контекст безопасности в SecurityContextHolder
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        
        // Сохраняем контекст в сессии
        securityContextRepository.saveContext(context, request, response);
        
        // Вызываем стандартный обработчик
        super.successfulAuthentication(request, response, chain, authResult);
    }
    
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {
        
        // При неудачной аутентификации генерируем новую капчу
        String captchaImage = captchaService.generateCaptchaImage();
        request.getSession().setAttribute("captchaImage", captchaImage);
        
        super.unsuccessfulAuthentication(request, response, failed);
    }
}
