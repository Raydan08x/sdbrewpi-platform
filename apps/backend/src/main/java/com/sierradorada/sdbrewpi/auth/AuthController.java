package com.sierradorada.sdbrewpi.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth")
class AuthController {
 private final AuthService service;AuthController(AuthService service){this.service=service;}
 @PostMapping("/login") ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.login(request));}
 @GetMapping("/session") ResponseEntity<SessionView> session(HttpServletRequest request){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.session(bearer(request)));}
 @PostMapping("/logout") ResponseEntity<Void> logout(HttpServletRequest request){service.logout(bearer(request));return ResponseEntity.noContent().build();}
 static String bearer(HttpServletRequest request){String h=request.getHeader("Authorization");return h!=null&&h.startsWith("Bearer ")?h.substring(7):"";}
}
