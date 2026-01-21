package com.application.service;

import com.application.entity.User;
import com.application.entity.UserRole;
import com.application.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User createUser(User user, String rawPassword) {
        // Validation des données
        validateUserData(user);
        
        // Vérification des doublons
        if (existsByUsername(user.getUsername())) {
            throw new DuplicateUserException("Username already exists: " + user.getUsername());
        }
        if (user.getEmail() != null && existsByEmail(user.getEmail())) {
            throw new DuplicateUserException("Email already exists: " + user.getEmail());
        }

        // Encodage du mot de passe
        user.setPassword(passwordEncoder.encode(rawPassword));
        
        // Valeurs par défaut
        if (user.getActive() == null) {
            user.setActive(true);
        }

        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User user) {
        User existingUser = findById(id);
        
        // Validation des données
        validateUserData(user);
        
        // Vérification des doublons pour username et email s'ils sont modifiés
        if (!existingUser.getUsername().equals(user.getUsername()) && existsByUsername(user.getUsername())) {
            throw new DuplicateUserException("Username already exists: " + user.getUsername());
        }
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail()) && existsByEmail(user.getEmail())) {
            throw new DuplicateUserException("Email already exists: " + user.getEmail());
        }

        // Mise à jour des champs
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setRole(user.getRole());
        existingUser.setActive(user.getActive());

        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    private void validateUserData(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (user.getUsername().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
    }
}
