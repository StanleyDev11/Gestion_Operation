package com.ace3i.connect.repository;

import com.ace3i.connect.entity.Intervener;
import com.ace3i.connect.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IntervenerRepository extends JpaRepository<Intervener, Long> {
    List<Intervener> findByClient(Client client);
    List<Intervener> findByClientId(Long clientId);
}
