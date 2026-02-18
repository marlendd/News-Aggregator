package com.newsaggregator.functional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.newsaggregator.entity.User;
import com.newsaggregator.repository.UserRepository;
import com.newsaggregator.service.UserService;

/**
 * Функциональные тесты для управления пользователями
 * Проверяют полные сценарии работы с пользователями
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("User Management Workflow Functional Tests")
class UserManagementWorkflowTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Полный сценарий регистрации и активации пользователя")
    void testUserRegistrationAndActivationWorkflow() {
        // 1. Регистрация нового пользователя
        String username = "newuser_" + System.currentTimeMillis();
        String email = username + "@test.com";
        
        User newUser = userService.createUser(username, email, "password123", "READER");
        
        assertNotNull(newUser);
        assertNotNull(newUser.getId());
        assertEquals(username, newUser.getUsername());
        assertEquals(email, newUser.getEmail());
        assertTrue(newUser.isEnabled());

        // 2. Проверка, что пользователь может войти в систему
        UserDetails userDetails = userService.loadUserByUsername(username);
        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());

        // 3. Деактивация пользователя
        userService.deactivateUser(newUser.getId());
        
        Optional<User> deactivatedUser = userService.findById(newUser.getId());
        assertTrue(deactivatedUser.isPresent());
        assertFalse(deactivatedUser.get().isEnabled());

        // 4. Повторная активация
        userService.activateUser(newUser.getId());
        
        Optional<User> reactivatedUser = userService.findById(newUser.getId());
        assertTrue(reactivatedUser.isPresent());
        assertTrue(reactivatedUser.get().isEnabled());
    }

    @Test
    @DisplayName("Сценарий изменения роли пользователя")
    void testUserRoleChangeWorkflow() {
        // 1. Создание пользователя с ролью READER
        String username = "roleuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, username + "@test.com", "password", "READER");
        
        // 2. Проверка начальной роли
        assertTrue(user.hasRole("READER"));
        assertFalse(user.hasRole("EDITOR"));

        // 3. Изменение роли на EDITOR
        userService.changeUserRole(user.getId(), "EDITOR");
        
        Optional<User> updatedUser = userService.findById(user.getId());
        assertTrue(updatedUser.isPresent());
        assertTrue(updatedUser.get().hasRole("EDITOR"));
        assertFalse(updatedUser.get().hasRole("READER"));

        // 4. Изменение роли на ADMIN
        userService.changeUserRole(user.getId(), "ADMIN");
        
        Optional<User> adminUser = userService.findById(user.getId());
        assertTrue(adminUser.isPresent());
        assertTrue(adminUser.get().hasRole("ADMIN"));
    }

    @Test
    @DisplayName("Сценарий проверки уникальности пользователя")
    void testUserUniquenessWorkflow() {
        // 1. Создание первого пользователя
        String username = "uniqueuser_" + System.currentTimeMillis();
        String email = username + "@test.com";
        
        userService.createUser(username, email, "password", "READER");

        // 2. Попытка создать пользователя с тем же именем
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(username, "different@test.com", "password", "READER");
        });

        // 3. Попытка создать пользователя с тем же email
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("differentuser", email, "password", "READER");
        });

        // 4. Создание пользователя с уникальными данными должно пройти успешно
        User uniqueUser = userService.createUser(
            "anotheruser_" + System.currentTimeMillis(),
            "another_" + System.currentTimeMillis() + "@test.com",
            "password",
            "READER"
        );
        assertNotNull(uniqueUser);
    }

    @Test
    @DisplayName("Сценарий поиска пользователей")
    void testUserSearchWorkflow() {
        // 1. Создание нескольких пользователей
        String prefix = "searchtest_" + System.currentTimeMillis();
        userService.createUser(prefix + "_user1", prefix + "_1@test.com", "pass", "READER");
        userService.createUser(prefix + "_user2", prefix + "_2@test.com", "pass", "READER");
        userService.createUser(prefix + "_user3", prefix + "_3@test.com", "pass", "EDITOR");

        // 2. Поиск по части имени
        Page<User> searchResults = userService.searchUsers(prefix, PageRequest.of(0, 10));
        assertTrue(searchResults.getTotalElements() >= 3);

        // 3. Поиск по email
        Page<User> emailResults = userService.searchUsers("@test.com", PageRequest.of(0, 10));
        assertTrue(emailResults.getTotalElements() >= 3);
    }

    @Test
    @DisplayName("Сценарий получения пользователей по роли")
    void testUsersByRoleWorkflow() {
        // 1. Создание пользователей с разными ролями
        String prefix = "roletest_" + System.currentTimeMillis();
        userService.createUser(prefix + "_reader1", prefix + "_r1@test.com", "pass", "READER");
        userService.createUser(prefix + "_reader2", prefix + "_r2@test.com", "pass", "READER");
        userService.createUser(prefix + "_editor1", prefix + "_e1@test.com", "pass", "EDITOR");

        // 2. Получение пользователей с ролью READER
        var readers = userService.findByRole("READER");
        assertTrue(readers.size() >= 2);
        readers.forEach(user -> assertTrue(user.hasRole("READER")));

        // 3. Получение пользователей с ролью EDITOR
        var editors = userService.findByRole("EDITOR");
        assertTrue(editors.size() >= 1);
        editors.forEach(user -> assertTrue(user.hasRole("EDITOR")));

        // 4. Подсчет пользователей по ролям
        long readerCount = userService.getUsersCountByRole("READER");
        long editorCount = userService.getUsersCountByRole("EDITOR");
        
        assertTrue(readerCount >= 2);
        assertTrue(editorCount >= 1);
    }

    @Test
    @DisplayName("Сценарий обновления данных пользователя")
    void testUserUpdateWorkflow() {
        // 1. Создание пользователя
        String username = "updateuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, username + "@test.com", "pass", "READER");

        // 2. Обновление данных
        String newUsername = "updated_" + username;
        String newEmail = "updated_" + username + "@test.com";
        
        User updatedUser = userService.updateUser(user.getId(), newUsername, newEmail, true);

        // 3. Проверка обновления
        assertEquals(newUsername, updatedUser.getUsername());
        assertEquals(newEmail, updatedUser.getEmail());
        assertTrue(updatedUser.isEnabled());

        // 4. Проверка через поиск
        Optional<User> foundUser = userService.findByUsername(newUsername);
        assertTrue(foundUser.isPresent());
        assertEquals(newEmail, foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Сценарий изменения пароля пользователя")
    void testPasswordChangeWorkflow() {
        // 1. Создание пользователя
        String username = "passuser_" + System.currentTimeMillis();
        User user = userService.createUser(username, username + "@test.com", "oldpass", "READER");
        
        String oldPassword = user.getPassword();

        // 2. Изменение пароля
        User updatedUser = userService.changePassword(user.getId(), "newpass123");

        // 3. Проверка, что пароль изменился
        assertNotEquals(oldPassword, updatedUser.getPassword());
        
        // 4. Проверка, что пользователь все еще может войти
        UserDetails userDetails = userService.loadUserByUsername(username);
        assertNotNull(userDetails);
    }

    @Test
    @DisplayName("Сценарий получения активных пользователей")
    void testActiveUsersWorkflow() {
        // 1. Создание активных и неактивных пользователей
        String prefix = "activetest_" + System.currentTimeMillis();
        
        User activeUser1 = userService.createUser(prefix + "_active1", prefix + "_a1@test.com", "pass", "READER");
        User activeUser2 = userService.createUser(prefix + "_active2", prefix + "_a2@test.com", "pass", "READER");
        User inactiveUser = userService.createUser(prefix + "_inactive", prefix + "_i@test.com", "pass", "READER");
        
        // 2. Деактивация одного пользователя
        userService.deactivateUser(inactiveUser.getId());

        // 3. Получение списка активных пользователей
        var activeUsers = userRepository.findByEnabledTrue();
        
        // 4. Проверка, что активные пользователи в списке
        assertTrue(activeUsers.stream().anyMatch(u -> u.getId().equals(activeUser1.getId())));
        assertTrue(activeUsers.stream().anyMatch(u -> u.getId().equals(activeUser2.getId())));
        
        // 5. Проверка, что неактивный пользователь не в списке
        assertFalse(activeUsers.stream().anyMatch(u -> u.getId().equals(inactiveUser.getId())));
    }

    @Test
    @DisplayName("Сценарий пагинации пользователей")
    void testUserPaginationWorkflow() {
        // 1. Создание нескольких пользователей
        String prefix = "pagetest_" + System.currentTimeMillis();
        for (int i = 1; i <= 5; i++) {
            userService.createUser(
                prefix + "_user" + i,
                prefix + "_" + i + "@test.com",
                "pass",
                "READER"
            );
        }

        // 2. Получение первой страницы (2 пользователя)
        Page<User> page1 = userService.findAll(PageRequest.of(0, 2));
        assertEquals(2, page1.getContent().size());
        assertTrue(page1.getTotalElements() >= 5);

        // 3. Получение второй страницы
        Page<User> page2 = userService.findAll(PageRequest.of(1, 2));
        assertEquals(2, page2.getContent().size());

        // 4. Проверка, что пользователи на разных страницах разные
        assertNotEquals(
            page1.getContent().get(0).getId(),
            page2.getContent().get(0).getId()
        );
    }
}
