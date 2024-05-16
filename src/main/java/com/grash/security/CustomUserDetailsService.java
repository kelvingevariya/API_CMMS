package com.grash.security;

import com.grash.model.OwnUser;
import com.grash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public CustomUserDetail loadUserByUsername(String username) throws UsernameNotFoundException {
        final OwnUser user = userRepository.findByEmail(username).get();
        if (user == null) {
            throw new UsernameNotFoundException("User '" + username + "' not found");
        }

        return CustomUserDetail.builder()//
                .user(user)//
                .build();
    }

}
