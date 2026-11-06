package com.ace3i.connect.config;

import com.ace3i.connect.entity.TicketCategory;
import com.ace3i.connect.repository.TicketCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initialise les catégories de tickets par défaut si la table est vide.
 */
@Component
public class TicketCategoryInitializer implements ApplicationRunner {

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (ticketCategoryRepository.count() == 0) {
            ticketCategoryRepository.save(new TicketCategory("Autre", 99));
        }
    }
}
