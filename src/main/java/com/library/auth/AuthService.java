package com.library.auth;

import com.library.auth.dto.AuthDtos;
import com.library.user.User;
import com.library.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LibrarianVerificationService librarianVerificationService;

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        // 查找用戶
        Optional<User> userOpt = userRepository.findByUsername(request.username());
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userOpt.get();
        
        // 驗證密碼
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // 檢查用戶是否啟用
        if (!user.isActive()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Account is disabled");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        return new AuthDtos.AuthResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getId()
        );
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        // 檢查用戶名是否已存在
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Username already exists");
        }

        // 驗證角色
        if (!"Librarian".equals(request.role()) && !"Member".equals(request.role())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid role. Must be 'Librarian' or 'Member'");
        }

        // 如果是館員，需要進行外部驗證
        if ("Librarian".equals(request.role())) {
            boolean isVerified = librarianVerificationService.verifyLibrarian(request.username());
            if (!isVerified) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Librarian verification failed");
            }
        }

        // 建立新用戶
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);

        User savedUser = userRepository.save(user);

        // 生成 JWT Token
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());

        return new AuthDtos.AuthResponse(
                token,
                savedUser.getUsername(),
                savedUser.getRole(),
                savedUser.getId()
        );
    }
}
