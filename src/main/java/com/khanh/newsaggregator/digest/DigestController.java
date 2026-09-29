package com.khanh.newsaggregator.digest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions/digest")
@RequiredArgsConstructor
public class DigestController {

    private final EmailDigestService emailDigestService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendDigest(Principal principal) {
        emailDigestService.sendDailyDigestToUser(principal.getName());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Daily digest email dispatched to " + principal.getName()
        ));
    }

    @GetMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewDigest(Principal principal) {
        String html = emailDigestService.previewDigestForUser(principal.getName());
        return ResponseEntity.ok(html);
    }
}
