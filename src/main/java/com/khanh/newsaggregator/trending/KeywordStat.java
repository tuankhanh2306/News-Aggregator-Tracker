package com.khanh.newsaggregator.trending;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "keyword_stat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(nullable = false)
    private int count;
}
