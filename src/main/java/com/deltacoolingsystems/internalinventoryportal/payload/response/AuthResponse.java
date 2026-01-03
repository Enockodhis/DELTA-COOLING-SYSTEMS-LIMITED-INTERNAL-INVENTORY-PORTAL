package com.deltacoolingsystems.internalinventoryportal.payload.response;

import com.deltacoolingsystems.internalinventoryportal.payload.dto.UserDto;
import lombok.Data;

@Data
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDto user;

}
