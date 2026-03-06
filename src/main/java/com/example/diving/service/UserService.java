package com.example.diving.service;

import com.example.diving.domain.User;
import com.example.diving.domain.UserRole;
import com.example.diving.dto.UserDto.*;
import com.example.diving.security.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class UserService {

    public UserResponse getProfile(String email) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = User.findByEmail(email);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        user.name  = request.name();
        user.phone = request.phone();
        user.persist();
        return UserResponse.from(user);
    }

    public List<UserResponse> getAllUsers() {
        return User.<User>listAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateRoles(Long userId, UpdateRolesRequest request) {
        User user = User.findById(userId);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        user.roles = new HashSet<>(request.roles());
        user.role  = user.primaryRole(); // synchroniser le rôle principal
        user.persist();
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (User.findByEmail(request.email()) != null) {
            throw new BadRequestException("Un compte existe déjà avec cet email");
        }
        User user = new User();
        user.email        = request.email();
        user.name         = request.name();
        user.phone        = request.phone();
        user.passwordHash = PasswordUtil.hash(request.password());
        user.roles        = new HashSet<>(request.roles());
        user.role         = user.primaryRole();
        user.persist();
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = User.findById(userId);
        if (user == null) throw new NotFoundException("Utilisateur non trouvé");
        user.delete();
    }
}
