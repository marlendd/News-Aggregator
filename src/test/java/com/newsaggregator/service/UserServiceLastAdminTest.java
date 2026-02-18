package com.newsaggregator.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.newsaggregator.entity.Role;
import com.newsaggregator.entity.User;
import com.newsaggregator.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceLastAdminTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setName("ADMIN");

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setEnabled(true);
        adminUser.setRoles(new HashSet<>(Arrays.asList(adminRole)));
    }

    @Test
    void testIsLastAdmin_WhenUserIsLastActiveAdmin_ShouldReturnTrue() {
        // Given
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        // When
        boolean result = userService.isLastAdmin(adminUser);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsLastAdmin_WhenMultipleActiveAdmins_ShouldReturnFalse() {
        // Given
        when(userRepository.countActiveAdmins()).thenReturn(2L);

        // When
        boolean result = userService.isLastAdmin(adminUser);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsLastAdmin_WhenUserIsNotAdmin_ShouldReturnFalse() {
        // Given
        Role readerRole = new Role();
        readerRole.setName("READER");
        
        User readerUser = new User();
        readerUser.setRoles(new HashSet<>(Arrays.asList(readerRole)));

        // When
        boolean result = userService.isLastAdmin(readerUser);

        // Then
        assertFalse(result);
    }

    @Test
    void testChangeUserRole_WhenLastAdminToNonAdmin_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.changeUserRole(1L, "READER");
        });

        assertEquals("Нельзя изменить роль последнего администратора в системе", exception.getMessage());
    }

    @Test
    void testDeactivateUser_WhenLastAdmin_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.deactivateUser(1L);
        });

        assertEquals("Нельзя деактивировать последнего администратора в системе", exception.getMessage());
    }

    @Test
    void testCanDeleteOrDeactivateUser_WhenLastAdmin_ShouldReturnFalse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        // When
        boolean result = userService.canDeleteOrDeactivateUser(1L);

        // Then
        assertFalse(result);
    }

    @Test
    void testCanChangeUserRole_WhenLastAdminToNonAdmin_ShouldReturnFalse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        // When
        boolean result = userService.canChangeUserRole(1L, "READER");

        // Then
        assertFalse(result);
    }

    @Test
    void testCanChangeUserRole_WhenLastAdminToAdmin_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        // When
        boolean result = userService.canChangeUserRole(1L, "ADMIN");

        // Then
        assertTrue(result);
    }
}