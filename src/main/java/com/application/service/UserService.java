package com.application.service;

import com.application.entity.User;
import com.application.entity.UserRole;

import java.util.List;

public interface UserService {

    User createUser(User user, String rawPassword);

    User updateUser(Long id, User user);

    void deleteUser(Long id);

    User findById(Long id);

    User findByUsername(String username);

    List<User> findAll();

    List<User> findByRole(UserRole role);

    List<User> findActiveUsers();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
