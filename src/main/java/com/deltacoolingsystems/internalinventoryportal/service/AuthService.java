package com.deltacoolingsystems.internalinventoryportal.service;

import com.deltacoolingsystems.internalinventoryportal.exceptions.UserException;
import com.deltacoolingsystems.internalinventoryportal.payload.dto.UserDto;
import com.deltacoolingsystems.internalinventoryportal.payload.response.AuthResponse;

public interface AuthService {
    AuthResponse signup(UserDto userDto) throws UserException;
    AuthResponse login(UserDto userDto) throws UserException;
    AuthResponse refreshToken(String oldToken) throws UserException;
    void logout(String token); // Add logout if implementing token blacklist
}