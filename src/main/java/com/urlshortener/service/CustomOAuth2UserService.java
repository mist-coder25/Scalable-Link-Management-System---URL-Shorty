package com.urlshortener.service;

import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        userRepository.findByEmail(email).ifPresentOrElse(
            user -> {
                // Update existing user
                user.setFullName(name);
                if (user.getProvider() == null) {
                    user.setProvider(provider);
                }
                userRepository.save(user);
            },
            () -> {
                // Create new user
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(name);
                newUser.setProvider(provider);
                newUser.setCreatedAt(LocalDateTime.now());
                // Set generic password for OAuth users since column is not null
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                userRepository.save(newUser);
            }
        );

        return oAuth2User;
    }
}
