package com.ace3i.connect.repository;

import com.ace3i.connect.entity.Client;
import com.ace3i.connect.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    // Recherche cloisonnée pour l'espace client (uniquement les tickets de son organisation)
    @Query("SELECT t FROM Ticket t WHERE t.client = :client AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.applicationName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Ticket> searchByClient(@Param("client") Client client,
                                @Param("status") String status,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    // Recherche globale pour l'espace technicien / administrateur ACE3i
    @Query("SELECT t FROM Ticket t WHERE " +
           "(:clientId IS NULL OR t.client.id = :clientId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:urgency IS NULL OR t.urgency = :urgency) AND " +
           "(:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.applicationName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.client.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Ticket> searchAll(@Param("clientId") Long clientId,
                           @Param("status") String status,
                           @Param("urgency") String urgency,
                           @Param("keyword") String keyword,
                           Pageable pageable);

    long countByClientAndStatus(Client client, String status);

    long countByStatus(String status);

    long countByUrgency(String urgency);

    long countByUrgencyAndStatusNotIn(String urgency, List<String> statuses);

    List<Ticket> findTop5ByClientOrderByCreatedAtDesc(Client client);

    List<Ticket> findTop5ByOrderByCreatedAtDesc();

    List<Ticket> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT t.client.name, COUNT(t) FROM Ticket t GROUP BY t.client.name ORDER BY COUNT(t) DESC")
    List<Object[]> countByClientGrouped();

    @Query("SELECT t.applicationName, COUNT(t) FROM Ticket t GROUP BY t.applicationName ORDER BY COUNT(t) DESC")
    List<Object[]> countByApplicationGrouped();

    Ticket findTopByOrderByIdDesc();
}
