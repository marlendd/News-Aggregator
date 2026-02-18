package com.newsaggregator.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.Role;
import com.newsaggregator.entity.User;
import com.newsaggregator.entity.UserPreferences;
import com.newsaggregator.repository.RoleRepository;
import com.newsaggregator.repository.UserPreferencesRepository;
import com.newsaggregator.repository.UserRepository;

/**
 * Сервис для управления пользователями в новостном агрегаторе.
 * 
 * Реализует интерфейс UserDetailsService для интеграции с Spring Security.
 * Предоставляет функциональность для:
 * - Аутентификации и авторизации пользователей
 * - CRUD операций с пользователями
 * - Управления ролями пользователей
 * - Защиты последнего администратора в системе
 * - Управления настройками пользователей
 * - Статистики и аналитики пользователей
 * 
 * @author News Aggregator Team
 * @version 1.0
 * @since 1.0
 */
@Service
@Transactional
public class UserService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Загружает пользователя по имени пользователя для Spring Security.
     * Реализация метода интерфейса UserDetailsService.
     * 
     * @param username имя пользователя
     * @return объект UserDetails для аутентификации
     * @throws UsernameNotFoundException если пользователь не найден
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
        
        logger.debug("Загружен пользователь: {} с ролями: {}", username, user.getRoles());
        return user;
    }
    
    /**
     * Находит пользователя по идентификатору.
     * 
     * @param id идентификатор пользователя
     * @return Optional с найденным пользователем или пустой Optional
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Находит пользователя по имени пользователя.
     * 
     * @param username имя пользователя
     * @return Optional с найденным пользователем или пустой Optional
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Находит пользователя по email адресу.
     * 
     * @param email email адрес пользователя
     * @return Optional с найденным пользователем или пустой Optional
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Получает всех пользователей без пагинации.
     * 
     * @return список всех пользователей
     */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    /**
     * Получает всех пользователей с пагинацией.
     * 
     * @param pageable параметры пагинации
     * @return страница пользователей
     */
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * Выполняет поиск пользователей по имени пользователя или email.
     * 
     * @param search поисковый запрос
     * @param pageable параметры пагинации
     * @return страница найденных пользователей
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String search, Pageable pageable) {
        return userRepository.findByUsernameContainingOrEmailContaining(search, pageable);
    }
    
    /**
     * Находит всех пользователей с определенной ролью.
     * 
     * @param roleName название роли
     * @return список пользователей с указанной ролью
     */
    @Transactional(readOnly = true)
    public List<User> findByRole(String roleName) {
        return userRepository.findByRoleName(roleName);
    }
    
    /**
     * Проверяет существование пользователя с указанным именем.
     * 
     * @param username имя пользователя для проверки
     * @return true, если пользователь существует, false в противном случае
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Проверяет существование пользователя с указанным email.
     * 
     * @param email email адрес для проверки
     * @return true, если пользователь существует, false в противном случае
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public User createUser(String username, String email, String password, String roleName) {
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Роль не найдена: " + roleName));
        
        User user = new User(username, email, passwordEncoder.encode(password));
        user.addRole(role);
        
        User savedUser = userRepository.save(user);
        
        // Создаем настройки пользователя по умолчанию
        UserPreferences preferences = new UserPreferences(savedUser);
        userPreferencesRepository.save(preferences);
        
        logger.info("Создан новый пользователь: {} с ролью: {}", username, roleName);
        return savedUser;
    }
    
    public User updateUser(Long userId, String username, String email, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        // Проверяем уникальность username и email
        if (!user.getUsername().equals(username) && existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        
        if (!user.getEmail().equals(email) && existsByEmail(email)) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(enabled);
        
        User savedUser = userRepository.save(user);
        logger.info("Обновлен пользователь: {}", username);
        return savedUser;
    }
    
    public User changeUserRole(Long userId, String newRoleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        // Проверяем, является ли пользователь последним администратором
        if (isLastAdmin(user) && !"ADMIN".equals(newRoleName)) {
            throw new IllegalStateException("Нельзя изменить роль последнего администратора в системе");
        }
        
        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new IllegalArgumentException("Роль не найдена: " + newRoleName));
        
        // Очищаем старые роли и добавляем новую
        user.getRoles().clear();
        user.addRole(newRole);
        
        User savedUser = userRepository.save(user);
        logger.info("Изменена роль пользователя {} на {}", user.getUsername(), newRoleName);
        return savedUser;
    }
    
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        // Проверяем, является ли пользователь последним администратором
        if (isLastAdmin(user)) {
            throw new IllegalStateException("Нельзя деактивировать последнего администратора в системе");
        }
        
        user.setEnabled(false);
        userRepository.save(user);
        logger.info("Деактивирован пользователь: {}", user.getUsername());
    }
    
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        user.setEnabled(true);
        userRepository.save(user);
        logger.info("Активирован пользователь: {}", user.getUsername());
    }
    
    public User changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);
        logger.info("Изменен пароль пользователя: {}", user.getUsername());
        return savedUser;
    }
    
    @Transactional(readOnly = true)
    public long getTotalUsersCount() {
        return userRepository.count();
    }
    
    @Transactional(readOnly = true)
    public long getUsersCountByRole(String roleName) {
        return userRepository.countByRoleName(roleName);
    }
    
    @Transactional(readOnly = true)
    public List<User> getRecentActiveUsers() {
        return userRepository.findRecentActiveUsers();
    }
    
    public void saveUserPreferences(User user, UserPreferences preferences) {
        preferences.setUser(user);
        userPreferencesRepository.save(preferences);
        logger.info("Сохранены настройки пользователя: {}", user.getUsername());
    }
    
    /**
     * Проверяет, является ли пользователь последним активным администратором в системе
     */
    @Transactional(readOnly = true)
    public boolean isLastAdmin(User user) {
        // Проверяем, является ли пользователь администратором
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));
        
        if (!isAdmin) {
            return false;
        }
        
        // Считаем количество активных администраторов
        long activeAdminCount = userRepository.countActiveAdmins();
        
        // Если пользователь активен и администраторов всего 1, то он последний
        return user.isEnabled() && activeAdminCount == 1;
    }
    
    /**
     * Проверяет, можно ли удалить или деактивировать пользователя
     */
    @Transactional(readOnly = true)
    public boolean canDeleteOrDeactivateUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        
        return !isLastAdmin(user);
    }
    
    /**
     * Проверяет, можно ли изменить роль пользователя
     */
    @Transactional(readOnly = true)
    public boolean canChangeUserRole(Long userId, String newRoleName) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        
        // Если пользователь последний админ и новая роль не админ - нельзя
        if (isLastAdmin(user) && !"ADMIN".equals(newRoleName)) {
            return false;
        }
        
        return true;
    }
}