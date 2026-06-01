package com.ace3i.connect.config;

import com.ace3i.connect.service.IAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    @Autowired
    private IAuditLogService auditLogService;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof User) {
            User user = (User) event.getAuthentication().getPrincipal();
            String username = user.getUsername();
            auditLogService.log("Connexion Réussie", "Utilisateur " + username + " s'est connecté.");
        }
    }
}
