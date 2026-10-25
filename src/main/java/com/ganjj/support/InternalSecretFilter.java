package com.ganjj.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class InternalSecretFilter extends OncePerRequestFilter {
    private final byte[] expected;
    public InternalSecretFilter(SupportConfig config) {
        expected=digest(config.secret());
    }
    private static byte[] digest(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String path=req.getServletPath();
        // Protect every route except the exact operational GETs. This also
        // rejects alternate path spellings (for example matrix parameters).
        boolean operational = "GET".equals(req.getMethod()) &&
                (path.equals("/health") || path.equals("/health/ready") || path.equals("/metrics"));
        if (!operational) {
            String secret=req.getHeader("x-internal-secret");
            if (secret==null || !MessageDigest.isEqual(expected,digest(secret))) {
                res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"acesso negado\"}"); return;
            }
        }
        chain.doFilter(req,res);
    }
}
