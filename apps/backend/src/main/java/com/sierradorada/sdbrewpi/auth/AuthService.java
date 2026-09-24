package com.sierradorada.sdbrewpi.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {
 private static final int ITERATIONS=120000;private static final Set<String> ROLES=Set.of("ADMIN","OPERATOR","VIEWER"); private final AuthRepository repository; private final SecureRandom random=new SecureRandom();
 AuthService(AuthRepository repository){this.repository=repository;}
 @Transactional LoginResponse login(LoginRequest request){
  UserCredential u=repository.user(request.username().trim()).orElseThrow(InvalidCredentialsException::new);
  byte[] calculated=derive(request.password().toCharArray(),Base64.getDecoder().decode(u.salt()));
  if(!MessageDigest.isEqual(calculated,Base64.getDecoder().decode(u.hash())))throw new InvalidCredentialsException();
  byte[] tokenBytes=new byte[32];random.nextBytes(tokenBytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);Instant now=Instant.now(),expires=now.plus(Duration.ofHours(12));
  repository.deleteExpired(now);repository.createSession(UUID.randomUUID().toString(),u.id(),hashToken(token),now,expires);
  return new LoginResponse(token,expires,new AuthUserView(u.id(),u.username(),u.displayName(),u.role()));
 }
 SessionView session(String token){AuthenticatedSession s=authenticate(token);return new SessionView(s.expiresAt(),s.user());}
 void logout(String token){repository.revoke(hashToken(token),Instant.now());}
 @Transactional AuthUserView register(UserRegistrationRequest request,AuthUserView actor){
  if(actor==null||!"ADMIN".equals(actor.role()))throw new UnauthorizedException();
  String username=request.username().trim().toLowerCase(Locale.ROOT),role=request.role().trim().toUpperCase(Locale.ROOT);
  if(username.chars().anyMatch(Character::isWhitespace))throw new IllegalArgumentException("El usuario no puede contener espacios");
  if(!ROLES.contains(role))throw new IllegalArgumentException("El rol no es válido");
  if(repository.usernameExists(username))throw new IllegalStateException("Ya existe un usuario con ese identificador");
  byte[] salt=new byte[16];random.nextBytes(salt);String id=UUID.randomUUID().toString();
  repository.createUser(id,username,request.displayName().trim(),role,Base64.getEncoder().encodeToString(salt),Base64.getEncoder().encodeToString(derive(request.password().toCharArray(),salt)),Instant.now());
  repository.audit(UUID.randomUUID().toString(),actor.username(),id,username+"; rol="+role);
  return new AuthUserView(id,username,request.displayName().trim(),role);
 }
 AuthenticatedSession authenticate(String token){if(token==null||token.isBlank())throw new UnauthorizedException();return repository.session(hashToken(token),Instant.now()).orElseThrow(UnauthorizedException::new);}
 private byte[] derive(char[] password,byte[] salt){try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(password,salt,ITERATIONS,256)).getEncoded();}catch(Exception e){throw new IllegalStateException("No fue posible validar las credenciales");}}
 private String hashToken(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("No fue posible validar la sesión");}}
}
