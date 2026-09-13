package com.joyeriaEcommerce.AureaTPO.config;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
@RestController
public class CsrfController {
 @GetMapping("/api/csrf") public java.util.Map<String,String> csrf(CsrfToken token){
  return java.util.Map.of("token",token.getToken(),"headerName",token.getHeaderName());
 }
}
