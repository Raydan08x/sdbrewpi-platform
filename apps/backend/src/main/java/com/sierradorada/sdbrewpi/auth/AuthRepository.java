package com.sierradorada.sdbrewpi.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AuthRepository {
 private final JdbcTemplate jdbc; AuthRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 Optional<UserCredential> user(String username){return jdbc.query("SELECT * FROM app_user WHERE LOWER(username)=LOWER(?) AND active=TRUE",(r,n)->new UserCredential(r.getString("id"),r.getString("username"),r.getString("display_name"),r.getString("role"),r.getString("password_salt"),r.getString("password_hash")),username).stream().findFirst();}
 void createSession(String id,String userId,String tokenHash,Instant now,Instant expires){jdbc.update("INSERT INTO auth_session VALUES (?,?,?,?,?,NULL)",id,userId,tokenHash,Timestamp.from(now),Timestamp.from(expires));}
 Optional<AuthenticatedSession> session(String tokenHash,Instant now){return jdbc.query("""
  SELECT s.id session_id,s.expires_at,u.id user_id,u.username,u.display_name,u.role FROM auth_session s
  JOIN app_user u ON u.id=s.user_id WHERE s.token_hash=? AND s.revoked_at IS NULL AND s.expires_at>? AND u.active=TRUE
  """,(r,n)->new AuthenticatedSession(r.getString("session_id"),r.getTimestamp("expires_at").toInstant(),new AuthUserView(r.getString("user_id"),r.getString("username"),r.getString("display_name"),r.getString("role"))),tokenHash,Timestamp.from(now)).stream().findFirst();}
 void revoke(String tokenHash,Instant now){jdbc.update("UPDATE auth_session SET revoked_at=? WHERE token_hash=? AND revoked_at IS NULL",Timestamp.from(now),tokenHash);}
 void deleteExpired(Instant now){jdbc.update("DELETE FROM auth_session WHERE expires_at<? OR revoked_at IS NOT NULL",Timestamp.from(now));}
}
