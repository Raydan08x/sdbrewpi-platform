package com.sierradorada.sdbrewpi.auth;

import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;

@Component
class AuthFilter extends OncePerRequestFilter {
 private final AuthService service;private final boolean enabled;AuthFilter(AuthService service,@Value("${sdbrewpi.auth.enabled:true}")boolean enabled){this.service=service;this.enabled=enabled;}
 @Override protected boolean shouldNotFilter(HttpServletRequest r){String p=r.getRequestURI();return !enabled||p.startsWith("/api/v1/auth/")||p.startsWith("/actuator/")||"OPTIONS".equals(r.getMethod());}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{try{AuthenticatedSession s=service.authenticate(AuthController.bearer(req));req.setAttribute("authUser",s.user());chain.doFilter(req,res);}catch(UnauthorizedException e){res.setStatus(401);res.setContentType("application/json");res.setCharacterEncoding("UTF-8");res.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Debes iniciar sesión para continuar\"}");}}
}
