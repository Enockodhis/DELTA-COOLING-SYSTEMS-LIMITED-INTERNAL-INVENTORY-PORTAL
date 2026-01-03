package com.deltacoolingsystems.internalinventoryportal.service;

import com.deltacoolingsystems.internalinventoryportal.exceptions.UserException;
import com.deltacoolingsystems.internalinventoryportal.modal.User;

import java.util.List;

public interface UserService {

    User getUserFromJwtToken(String jwtToken) throws UserException;
    User getCurrentUser() throws UserException;
    User getUserByEmail(String email) throws UserException;
    User getUserById(Long id);
    List<User> getAllUsers();

}
