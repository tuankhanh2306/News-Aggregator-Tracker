package com.khanh.newsaggregator.source;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "source")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "rss_url", nullable = false, unique = true, length = 500)
    private String rssUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;
}
