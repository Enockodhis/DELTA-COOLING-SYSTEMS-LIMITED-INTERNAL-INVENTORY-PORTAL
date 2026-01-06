package com.deltacoolingsystems.internalinventoryportal.mapper;

import com.deltacoolingsystems.internalinventoryportal.modal.User;
import com.deltacoolingsystems.internalinventoryportal.payload.dto.UserDto;

public class UserMapper {

    public static UserDto toDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setFirstName(user.getFirstName());      // ADD THIS
        userDto.setLastName(user.getLastName());
        userDto.setFullName(user.getFullName());
        userDto.setEmail(user.getEmail());
        userDto.setPhone(user.getPhone());
        userDto.setRole(user.getRole());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());      // ADD THIS
        userDto.setLastLogin(user.getLastLogin());

        // NEVER include password in DTO response!
        // userDto.setPassword(null); // Already null by default

        return userDto;
    }

    // Optional: Add a toEntity method if you need it
    public static User toEntity(UserDto userDto) {
        if (userDto == null) {
            return null;
        }

        User user = new User();
        user.setFirstName(userDto.getFirstName());
//        user.setFullName(userDto.getFullName());  Add if you have FullName
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setRole(userDto.getRole());
        // Don't set password here - should be handled in service with encoding
        // Don't set timestamps here - should be handled in entity lifecycle

        return user;
    }
}