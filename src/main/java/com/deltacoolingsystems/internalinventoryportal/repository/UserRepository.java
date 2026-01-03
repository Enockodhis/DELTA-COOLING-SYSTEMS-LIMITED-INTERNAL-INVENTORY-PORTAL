package com.deltacoolingsystems.internalinventoryportal.repository;

import com.deltacoolingsystems.internalinventoryportal.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

}
