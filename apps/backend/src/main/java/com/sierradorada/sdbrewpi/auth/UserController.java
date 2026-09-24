package com.sierradorada.sdbrewpi.auth;

import jakarta.servlet.http.HttpServletRequest;import jakarta.validation.Valid;import java.net.URI;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/users")
class UserController {
 private final AuthService service;UserController(AuthService service){this.service=service;}
 @PostMapping ResponseEntity<AuthUserView> register(@Valid @RequestBody UserRegistrationRequest request,HttpServletRequest servletRequest){AuthUserView created=service.register(request,(AuthUserView)servletRequest.getAttribute("authUser"));return ResponseEntity.created(URI.create("/api/v1/users/"+created.id())).body(created);}
}
