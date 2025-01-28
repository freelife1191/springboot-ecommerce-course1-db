package com.onion.backend.controller;

import com.onion.backend.dto.SignUpUser;
import com.onion.backend.entity.User;
import com.onion.backend.jwt.JwtUtil;
import com.onion.backend.service.CustomUserDetailsService;
import com.onion.backend.service.JwtBlacklistService;
import com.onion.backend.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    @GetMapping("")
    public ResponseEntity<List<User>> getUserS() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @PostMapping("/signUp")
    public ResponseEntity<User> createUser(@RequestBody SignUpUser signUpUser) {
        User user = userService.createUser(signUpUser);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to be deleted", example = "onion", required = true)
            @PathVariable("userId") Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public String login(
            @RequestParam(value = "username", defaultValue = "onion") String username,
            @RequestParam(value = "password", defaultValue = "onion1!") String password,
            HttpServletResponse response
            ) throws AuthenticationException {
        // Spring Security에서 제공하는 AuthenticationManager를 사용하여 인증을 수행
        // 인증에 성공하면 UserDetails를 반환하고, 실패하면 AuthenticationException을 발생
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        // JWT 토큰 생성 후 응답
        // return jwtUtil.generateToken(userDetails.getUsername());

        String token = jwtUtil.generateToken(userDetails.getUsername());
        // 쿠키에 토큰 저장
        Cookie cookie = new Cookie("onion_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);

        response.addCookie(cookie);
        return token;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("onion_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키 삭제
        response.addCookie(cookie);
    }

    @PostMapping("/logout/all")
    public void logout(
            @RequestParam(required = false) String requestToken,
            @CookieValue(value = "onion_token", required = false) String cookieToken,
            HttpServletRequest request, HttpServletResponse response
    ) {
        String token = null;
        String bearerToken = request.getHeader("Authorization");
        if (requestToken != null) {
            // 요청 파라미터에서 토큰을 가져옴
            token = requestToken;
        } else if (cookieToken != null) {
            // 쿠키에서 토큰을 가져옴
            token = cookieToken;
        } else if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // 헤더에서 토큰을 가져옴
            token = bearerToken.substring(7);
        }
        Instant instant = new Date().toInstant();
        // 현재 시간 기준으로 만료 시간 설정
        LocalDateTime expirationTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        String username = jwtUtil.getUsernameFromToken(token);
        // 토큰을 블랙리스트에 추가
        jwtBlacklistService.blacklistToken(token, expirationTime, username);
        Cookie cookie = new Cookie("onion_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키 삭제
        response.addCookie(cookie);
    }

    @PostMapping("/token/validation")
    @ResponseStatus(HttpStatus.OK)
    public void jwtValidate(@RequestParam("token") String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token is not validation");
        }
    }
}
