package com.miniclub.service;

import com.miniclub.dto.AuthResponse;
import com.miniclub.dto.LoginRequest;
import com.miniclub.dto.RegisterRequest;
import com.miniclub.dto.UserDto;
import com.miniclub.exception.ApiException;
import com.miniclub.model.Role;
import com.miniclub.model.User;
import com.miniclub.repository.UserRepository;
import com.miniclub.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Public sign-up always creates a STUDENT account. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
        }
        User user = new User(req.name().trim(), email, passwordEncoder.encode(req.password()), Role.STUDENT,
                Text.clean(req.department()), req.year());
        userRepository.save(user);
        return new AuthResponse(jwtService.createToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                // Same message for unknown email and wrong password, so nobody can probe which accounts exist.
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        return new AuthResponse(jwtService.createToken(user), UserDto.from(user));
    }
}
