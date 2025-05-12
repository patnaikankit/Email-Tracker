package com.example.emailtracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.*;
import jakarta.mail.internet.MimeMessage;
import me.paulschwarz.springdotenv.DotenvPropertySource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@DotenvPropertySource
public class EmailTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmailTrackerApplication.class, args);
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(System.getenv("EMAIL_HOST"));
        mailSender.setPort(Integer.parseInt(System.getenv("EMAIL_PORT")));
        mailSender.setUsername(System.getenv("EMAIL_USERNAME"));
        mailSender.setPassword(System.getenv("EMAIL_PASSWORD"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.debug", "true");

        return mailSender;
    }
}

@RestController
@RequestMapping("/email")
class EmailController {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${tracking.domain}")
    private String trackingDomain;

    public EmailController(JavaMailSender mailSender, StringRedisTemplate redisTemplate) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/send")
    public Map<String, String> sendEmail(@RequestBody EmailRequest request) {
        Map<String, String> statusMap = new HashMap<>();

        for (Recipient recipient : request.getRecipients()) {
            String trackingId = UUID.randomUUID().toString();
            String pixelUrl = "<img src='" + trackingDomain + "/email/pixel/" + trackingId + "' width='1' height='1' style='display:none'>";
            String emailContent = request.getHtmlTemplate().replace("{{TRACKING_PIXEL}}", pixelUrl);

            try {
                MimeMessage message = mailSender.createMimeMessage();
                message.setFrom(request.getFrom());
                message.setRecipients(jakarta.mail.Message.RecipientType.TO, recipient.getEmail());
                message.setSubject(request.getSubject());
                message.setContent(emailContent, "text/html");
                mailSender.send(message);

                saveTrackingInfo(trackingId, recipient.getEmail());
                statusMap.put(recipient.getEmail(), "Success: " + trackingId);
            } catch (Exception e) {
                statusMap.put(recipient.getEmail(), "Failed: " + e.getMessage());
            }
        }
        return statusMap;
    }

    private void saveTrackingInfo(String trackingId, String email) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set("tracking:" + trackingId, email + ":0:" + Instant.now().toString(), 7, TimeUnit.DAYS);
    }

    @GetMapping("/pixel/{trackingId}")
    public byte[] trackEmail(@PathVariable String trackingId) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String data = ops.get("tracking:" + trackingId);
        if (data == null) return new byte[0];

        String[] parts = data.split(":");
        String email = parts[0];
        int count = Integer.parseInt(parts[1]) + 1;
        ops.set("tracking:" + trackingId, email + ":" + count + ":" + Instant.now().toString(), 7, TimeUnit.DAYS);

        return Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
    }

    @GetMapping("/status/{trackingId}")
    public Map<String, Object> getTrackingStatus(@PathVariable String trackingId) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String data = ops.get("tracking:" + trackingId);
        if (data == null) return Collections.singletonMap("error", "Tracking ID not found");

        String[] parts = data.split(":");
        return Map.of("email", parts[0], "count", Integer.parseInt(parts[1]), "last_opened", parts[2]);
    }
}
