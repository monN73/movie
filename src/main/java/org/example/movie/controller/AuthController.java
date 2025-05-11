package org.example.movie.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.movie.dto.UserDto;
import org.example.movie.entity.User;
import org.example.movie.repository.UserRepository;
import org.example.movie.security.JwtUtil;
import org.example.movie.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // 登录成功后获取用户信息
            User user = userRepository.findUserByName(username).orElseThrow(() -> new RuntimeException("用户不存在"));
            UserDto userDto = new UserDto();
            userDto.setUsername(user.getName());
            userDto.setAvatarUrl(user.getAvatarUrl());
            userDto.setPosterUrl(user.getPosterUrl());
            userDto.setId(user.getUid());

            // 生成 Token
            String token = jwtUtil.generateToken(username);

            // 返回 token 和用户信息
            return ResponseEntity.ok().body(Map.of(
                    "token", token,
                    "user", userDto
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("用户名或密码错误");
        }
    }

}
