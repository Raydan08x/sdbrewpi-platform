package com.sierradorada.sdbrewpi.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.http.MediaType;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.web.servlet.MockMvc;import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties="sdbrewpi.auth.enabled=true") @AutoConfigureMockMvc @Transactional
class AuthControllerTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
 @BeforeEach void testUser(){jdbc.update("INSERT INTO app_user VALUES ('auth-test-user','qa-admin','QA Admin','ADMIN','i0TefLxkNMDRiXisylWWDQ==','20ki3fGdGqAg8mKebXtURXfSwqWvQ75+bey8lsEUu3o=',TRUE,CURRENT_TIMESTAMP)");}
 @Test void authenticatesAdministratorAndProtectsApi()throws Exception{
  mvc.perform(get("/api/v1/plant/overview")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  String body=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"qa-admin\",\"password\":\"test-secret\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ADMIN")).andReturn().getResponse().getContentAsString();
  String token=json.readTree(body).get("token").asText();
  mvc.perform(get("/api/v1/auth/session").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.user.username").value("qa-admin"));
  mvc.perform(get("/api/v1/plant/overview").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  mvc.perform(post("/api/v1/users").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"Operador1\",\"displayName\":\"Operador Uno\",\"role\":\"OPERATOR\",\"password\":\"clave-segura-qa\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("operador1")).andExpect(jsonPath("$.role").value("OPERATOR"));
  mvc.perform(post("/api/v1/users").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"operador1\",\"displayName\":\"Duplicado\",\"role\":\"VIEWER\",\"password\":\"clave-segura-qa\"}"))
   .andExpect(status().isConflict());
  mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"operador1\",\"password\":\"clave-segura-qa\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("OPERATOR"));
  mvc.perform(post("/api/v1/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/auth/session").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
 }
 @Test void requiresAuthenticationAndAdminRoleToRegisterUsers()throws Exception{
  String request="{\"username\":\"viewer1\",\"displayName\":\"Visor Uno\",\"role\":\"VIEWER\",\"password\":\"clave-segura-qa\"}";
  mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
  String adminBody=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"qa-admin\",\"password\":\"test-secret\"}")).andReturn().getResponse().getContentAsString();
  String adminToken=json.readTree(adminBody).get("token").asText();
  mvc.perform(post("/api/v1/users").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isCreated());
  String viewerBody=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"viewer1\",\"password\":\"clave-segura-qa\"}")).andReturn().getResponse().getContentAsString();
  String viewerToken=json.readTree(viewerBody).get("token").asText();
  mvc.perform(post("/api/v1/users").header("Authorization","Bearer "+viewerToken).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
 }
 @Test void rejectsWrongPassword()throws Exception{mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"qa-admin\",\"password\":\"incorrecta\"}")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));}
}
