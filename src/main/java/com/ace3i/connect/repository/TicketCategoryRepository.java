package com.ace3i.connect.repository;

import com.ace3i.connect.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {
    List<TicketCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<TicketCategory> findAllByOrderByDisplayOrderAscNameAsc();
    boolean existsByName(String name);
}
