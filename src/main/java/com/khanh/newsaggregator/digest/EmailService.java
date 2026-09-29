package com.khanh.newsaggregator.digest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${news.digest.from:news-digest@newsaggregator.com}")
    private String fromAddress;

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> templateModel) {
        log.info("Preparing to send email to {} with subject: '{}'", to, subject);

        try {
            Context context = new Context();
            context.setVariables(templateModel);
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to build or send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    public String renderEmailPreview(String templateName, Map<String, Object> templateModel) {
        Context context = new Context();
        context.setVariables(templateModel);
        return templateEngine.process(templateName, context);
    }
}
