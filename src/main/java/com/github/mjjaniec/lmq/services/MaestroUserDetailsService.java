package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.stores.MaestroStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaestroUserDetailsService implements UserDetailsService {

    private final MaestroStore maestroStore;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return maestroStore
                .findByEmail(email)
                .map(maestro -> User.withUsername(maestro.email())
                        .password("")
                        .roles("MAESTRO")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
