package com.ace3i.connect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "CNNOT")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTID")
    private Long id;

    @Column(name = "NOTTIT")
    private String title;

    @Column(name = "NOTMSG")
    private String message;

    @Column(name = "NOTTYP")
    private String type; // info, success, warning

    @Column(name = "NOTTIM")
    private LocalDateTime timestamp;

    @Column(name = "NOTREA")
    private boolean isRead = false;

    @Column(name = "NOTURL")
    private String url; // Lien vers la mission concernée par exemple

    public Notification(String title, String message, String type, String url) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.url = url;
        this.timestamp = LocalDateTime.now();
    }
}
