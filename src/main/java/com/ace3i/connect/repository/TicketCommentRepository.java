package com.ace3i.connect.repository;

import com.ace3i.connect.entity.Ticket;
import com.ace3i.connect.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    // Pour l'espace technicien : tous les commentaires y compris les notes internes
    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);

    // Pour l'espace client : uniquement les commentaires publics
    List<TicketComment> findByTicketAndInternalOnlyFalseOrderByCreatedAtAsc(Ticket ticket);
}
