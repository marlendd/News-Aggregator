package com.newsaggregator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.Category;
import com.newsaggregator.entity.NewsSource;
import com.newsaggregator.entity.Role;
import com.newsaggregator.entity.User;
import com.newsaggregator.repository.CategoryRepository;
import com.newsaggregator.repository.NewsSourceRepository;
import com.newsaggregator.repository.RoleRepository;
import com.newsaggregator.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private NewsSourceRepository newsSourceRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Инициализация базовых данных...");
        
        initializeRoles();
        initializeUsers();
        initializeCategories();
        initializeNewsSources();
        
        logger.info("Инициализация базовых данных завершена");
    }
    
    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            logger.info("Создание ролей...");
            
            Role adminRole = new Role("ADMIN", "Администратор системы");
            Role editorRole = new Role("EDITOR", "Редактор контента");
            Role readerRole = new Role("READER", "Читатель");
            
            roleRepository.save(adminRole);
            roleRepository.save(editorRole);
            roleRepository.save(readerRole);
            
            logger.info("Роли созданы: ADMIN, EDITOR, READER");
        }
    }
    
    @Transactional
    private void initializeUsers() {
        if (userRepository.count() == 0) {
            logger.info("Создание пользователей по умолчанию...");
            
            // Администратор
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow(() -> new RuntimeException("ADMIN role not found"));
            User admin = new User("admin", "admin@newsaggregator.com", passwordEncoder.encode("admin123"));
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
            
            // Редактор
            Role editorRole = roleRepository.findByName("EDITOR").orElseThrow(() -> new RuntimeException("EDITOR role not found"));
            User editor = new User("editor", "editor@newsaggregator.com", passwordEncoder.encode("editor123"));
            editor.getRoles().add(editorRole);
            userRepository.save(editor);
            
            // Читатель
            Role readerRole = roleRepository.findByName("READER").orElseThrow(() -> new RuntimeException("READER role not found"));
            User reader = new User("reader", "reader@newsaggregator.com", passwordEncoder.encode("reader123"));
            reader.getRoles().add(readerRole);
            userRepository.save(reader);
            
            logger.info("Пользователи созданы:");
            logger.info("  admin/admin123 (ADMIN)");
            logger.info("  editor/editor123 (EDITOR)");
            logger.info("  reader/reader123 (READER)");
        }
    }
    
    private void initializeCategories() {
        if (categoryRepository.count() == 0) {
            logger.info("Создание категорий...");
            
            Category[] categories = {
                new Category("Технологии", "Новости из мира технологий и IT", "#007bff"),
                new Category("Политика", "Политические новости и события", "#dc3545"),
                new Category("Экономика", "Экономические новости и аналитика", "#28a745"),
                new Category("Спорт", "Спортивные новости и результаты", "#fd7e14"),
                new Category("Наука", "Научные открытия и исследования", "#6f42c1"),
                new Category("Культура", "Культурные события и искусство", "#e83e8c"),
                new Category("Здоровье", "Новости медицины и здравоохранения", "#20c997"),
                new Category("Образование", "Новости образования и науки", "#6c757d"),
                new Category("Общество", "Общественные события и социальные вопросы", "#ffc107"),
                new Category("Мир", "Международные новости", "#17a2b8")
            };
            
            for (Category category : categories) {
                categoryRepository.save(category);
            }
            
            logger.info("Создано {} категорий", categories.length);
        }
    }
    
    private void initializeNewsSources() {
        if (newsSourceRepository.count() == 0) {
            logger.info("Создание источников новостей...");
            
            NewsSource[] sources = {
                new NewsSource("Хабр", "https://habr.com/ru/rss/hub/programming/", "https://habr.com"),
                new NewsSource("РИА Новости", "https://ria.ru/export/rss2/archive/index.xml", "https://ria.ru"),
                new NewsSource("Лента.ру", "https://lenta.ru/rss", "https://lenta.ru"),
                new NewsSource("Газета.ру", "https://www.gazeta.ru/export/rss/first.xml", "https://gazeta.ru"),
                new NewsSource("Ведомости", "https://www.vedomosti.ru/rss/news", "https://www.vedomosti.ru")
            };
            
            for (NewsSource source : sources) {
                newsSourceRepository.save(source);
            }
            
            logger.info("Создано {} источников новостей", sources.length);
        }
    }
}