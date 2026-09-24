package com.sierradorada.sdbrewpi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

record LoginRequest(@NotBlank String username,@NotBlank String password){}
record AuthUserView(String id,String username,String displayName,String role){}
record LoginResponse(String token,Instant expiresAt,AuthUserView user){}
record SessionView(Instant expiresAt,AuthUserView user){}
record AuthenticatedSession(String sessionId,Instant expiresAt,AuthUserView user){}
record UserCredential(String id,String username,String displayName,String role,String salt,String hash){}
record UserRegistrationRequest(@NotBlank @Size(min=3,max=160) String username,@NotBlank @Size(min=2,max=160) String displayName,@NotBlank String role,@NotBlank @Size(min=8,max=128) String password){}
