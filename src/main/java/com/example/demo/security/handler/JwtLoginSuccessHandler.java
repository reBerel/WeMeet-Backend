package com.example.demo.security.handler;

import com.example.demo.security.service.RedisService;
import com.example.demo.security.utils.JwtUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String role = getUserRole(authentication);

        long accessTokenExpMin = 10;
        String accessToken = jwtUtil.generateToken(Map.of("email", username), accessTokenExpMin);
        redisService.setKeyAndValue(accessToken, username, accessTokenExpMin+1);

        long refreshTokenExpMin = 60;
        String refreshToken = jwtUtil.generateToken(Map.of("email", username), refreshTokenExpMin);
        redisService.setKeyAndValue(refreshToken, username, refreshTokenExpMin+1);

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        String jsonStr = gson.toJson(Map.of("accessToken", accessToken, "refreshToken", refreshToken));

        out.write(jsonStr);
        out.flush();
        out.close();
    }

    private String getUserRole(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        GrantedAuthority grantedAuthority = authorities.stream().findFirst().orElseThrow();
        String authority = grantedAuthority.getAuthority();
        return authority;
    }
}
