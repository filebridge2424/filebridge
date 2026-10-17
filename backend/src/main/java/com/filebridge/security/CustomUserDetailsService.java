package com.filebridge.security;

import com.filebridge.model.User;
import com.filebridge.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new UserPrincipal(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getRole()
        );
    }

    public UserPrincipal loadUserById(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return new UserPrincipal(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getRole()
        );
    }
}
