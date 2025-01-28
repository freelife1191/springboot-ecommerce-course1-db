package com.onion.backend.service;

import com.onion.backend.entity.JwtBlacklist;
import com.onion.backend.jwt.JwtUtil;
import com.onion.backend.repository.JwtBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final JwtBlacklistRepository jwtBlacklistRepository;
    private final JwtUtil jwtUtil;

    /**
     * 토큰 Blacklist 추가
     * @param token
     * @param expirationTime
     * @param username
     */
    public void blacklistToken(String token, LocalDateTime expirationTime, String username) {
        Optional<JwtBlacklist> blacklistToken = jwtBlacklistRepository.findByToken(token);
        if (blacklistToken.isPresent()) {
            log.info("Token already blacklisted");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "blacklisted token");
        }
        JwtBlacklist jwtBlacklist = new JwtBlacklist();
        jwtBlacklist.setToken(token);
        jwtBlacklist.setExpirationTime(expirationTime);
        jwtBlacklist.setUsername(username);
        jwtBlacklistRepository.save(jwtBlacklist);
    }

    /**
     * 현재 토큰을 Blacklist 토큰으로 추가 하고 현재 토큰으로는 인증 할 수 없게 함
     * 현재 토큰 만료일시 1시간 이전 시간이 Blacklist 토큰 만료일시보다 이후 인지 확인
     * 다시 로그인 하기 위해서는 토큰을 다시 발급 받아야 함
     * @param currentToken
     * @return
     */
    public boolean isTokenBlacklisted(String currentToken) {
        String username = jwtUtil.getUsernameFromToken(currentToken);
        // 가장 최근에 만료 요청했던 Blacklist 토큰 조회
        Optional<JwtBlacklist> blacklistedToken = jwtBlacklistRepository.findTopByUsernameOrderByExpirationTime(username);
        if (blacklistedToken.isEmpty()) {
            return false;
        }
        // 현재 토큰 만료일시
        Instant instant = jwtUtil.getExpirationDateFromToken(currentToken).toInstant();
        // 토큰 만료일시를 LocalDateTime 으로 변환
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        // Blacklist 토큰 만료 일시 7:30 분
        // 현재 토큰 만료일시 8시 27분 이면 1시간 이전 시간은 7시 27분 이므로 7시 30분 이후 부터 해당 유저의 토큰 다시 발급 가능
        // 현재 요청을 한 상태에서 현재 토큰이 생성이 된 시간을 기준으로 1시간 이전 시간이 Blacklist 토큰 만료일시보다 이후 인지 확인
        return blacklistedToken.get().getExpirationTime().isAfter(localDateTime.minusMinutes(60));
    }
}
