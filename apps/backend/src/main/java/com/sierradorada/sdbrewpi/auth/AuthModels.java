package com.sierradorada.sdbrewpi.auth;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

record LoginRequest(@NotBlank String username,@NotBlank String password){}
record AuthUserView(String id,String username,String displayName,String role){}
record LoginResponse(String token,Instant expiresAt,AuthUserView user){}
record SessionView(Instant expiresAt,AuthUserView user){}
record AuthenticatedSession(String sessionId,Instant expiresAt,AuthUserView user){}
record UserCredential(String id,String username,String displayName,String role,String salt,String hash){}
