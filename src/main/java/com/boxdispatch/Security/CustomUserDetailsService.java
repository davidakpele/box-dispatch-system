package com.boxdispatch.Security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boxdispatch.Models.Users;
import com.boxdispatch.Repositories.UsersRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    public CustomUserDetailsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }
  
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
   
        Users user = usersRepository.findByUsername(username)
            .orElseThrow(() -> {
                return new UsernameNotFoundException("User not found with username: " + username);
            });
        return user;
    }
}
