package com.ace3i.connect.repository;

import com.ace3i.connect.entity.Ticket;
import com.ace3i.connect.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicket(Ticket ticket);
}
