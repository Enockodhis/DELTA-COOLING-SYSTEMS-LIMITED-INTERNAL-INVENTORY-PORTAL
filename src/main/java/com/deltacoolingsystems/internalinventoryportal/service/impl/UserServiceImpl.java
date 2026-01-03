package com.deltacoolingsystems.internalinventoryportal.service.impl;

import com.deltacoolingsystems.internalinventoryportal.configuration.JwtProvider;
import com.deltacoolingsystems.internalinventoryportal.exceptions.UserException;
import com.deltacoolingsystems.internalinventoryportal.modal.User;
import com.deltacoolingsystems.internalinventoryportal.repository.UserRepository;
import com.deltacoolingsystems.internalinventoryportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public User getUserFromJwtToken(String jwtToken) throws UserException {
        String email = jwtProvider.getEmailFromToken(jwtToken); // Changed "token" to "jwtToken"
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("Invalid token");
        }
        return user;
    }

    @Override
    public User getCurrentUser() throws UserException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("User not found");
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) throws UserException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("User not found");
        }
        return user;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public List<User> getAllUsers() {
        return List.of();
    }
}
