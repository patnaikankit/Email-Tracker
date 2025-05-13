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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class EmailTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmailTrackerApplication.class, args);
    }

    @Value("${EMAIL_HOST}")
    private String emailHost;

    @Value("${EMAIL_PORT}")
    private String emailPort;

    @Value("${EMAIL_USERNAME}")
    private String emailUsername;

    @Value("${EMAIL_PASSWORD}")
    private String emailPassword;

    @Value("${TRACKING_DOMAIN}")
    private String trackingDomain;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailHost);
        mailSender.setPort(Integer.parseInt(emailPort));
        mailSender.setUsername(emailUsername);
        mailSender.setPassword(emailPassword);

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
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${TRACKING_DOMAIN}")
    private String trackingDomain;

    public EmailController(JavaMailSender mailSender, StringRedisTemplate redisTemplate) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/send")
    public Map<String, Object> sendEmail(@RequestBody EmailRequest request) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> status = new HashMap<>();

        for (Receiver receiver : request.getRecipients().getReceivers()) {
            if (receiver.isWantToTrack()) {
                String trackingId = UUID.randomUUID().toString();
                String domain = trackingDomain.startsWith("http") ? trackingDomain : "http://" + trackingDomain;
                String pixelUrl = String.format("<img src='%s/email/pixel/%s' width='1' height='1' style='display:none' alt='' />", 
                    domain, trackingId);
                
                String emailContent = request.getEmailBody().getHtmlTemplate();
                Map<String, String> parameters = request.getEmailBody().getParameters().get(receiver.getEmail());
                if (parameters != null) {
                    for (Map.Entry<String, String> param : parameters.entrySet()) {
                        emailContent = emailContent.replace("{{ " + param.getKey() + " }}", param.getValue());
                    }
                }
                emailContent = emailContent.replace("{{TRACKING_PIXEL}}", pixelUrl);

                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    message.setFrom(request.getRecipients().getFrom());
                    message.setRecipients(jakarta.mail.Message.RecipientType.TO, receiver.getEmail());
                    message.setSubject(request.getEmailBody().getSubject());
                    message.setContent(emailContent, "text/html");
                    mailSender.send(message);

                    logger.info("Mail Sender: {}", mailSender);

                    String createdAt = Instant.now().toString();
                    String key = "tracking:" + trackingId;
                    String value = String.format("%s|0|%s|%s", receiver.getEmail(), createdAt, createdAt);

                    logger.info("Storing initial tracking data - Key: {}, Value: {}", key, value);
                    
                    redisTemplate.opsForValue().set(key, value, 7, TimeUnit.DAYS);
                    logger.info("Successfully saved tracking info to Redis");

                    status.put(receiver.getEmail(), "Success:" + trackingId);
                } catch (Exception e) {
                    logger.error("Failed to send email to {}: {}", receiver.getEmail(), e.getMessage());
                    status.put(receiver.getEmail(), "Failed: " + e.getMessage());
                }
            }
        }
        response.put("status", status);
        return response;
    }

    @GetMapping("/pixel/{trackingId}")
    public byte[] trackEmail(@PathVariable String trackingId) {
        String key = "tracking:" + trackingId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String data = ops.get(key);

        logger.info("Raw data from Redis for key {}: {}", key, data);

        if (data == null) {
            logger.warn("No tracking data found for ID: {}", trackingId);
            return new byte[0];
        }

        String[] parts = data.split("\\|");
        logger.info("Split parts: {}", Arrays.toString(parts));
        logger.info("Number of parts: {}", parts.length);

        if (parts.length != 4) {
            logger.error("Invalid tracking data format for ID {}: {}. Expected 8 parts but got {}", 
                trackingId, data, parts.length);
            return new byte[0];
        }

        String email = parts[0];
        int count;
        try {
            count = Integer.parseInt(parts[1]) + 1;
            logger.info("Successfully parsed count. Old value: {}, New value: {}", parts[1], count);
        } catch (NumberFormatException e) {
            logger.warn("Invalid count format for ID {}, resetting to 1. Original value: {}", trackingId, parts[1]);
            count = 1;
        }

        String lastOpened = Instant.now().toString();
        String createdAt = parts[2];

        logger.info("Email: {}, Last Opened: {}, Created At: {}", email, createdAt, lastOpened);

        // Update the value and reset the 7-day TTL
        String newValue = String.format("%s|%d|%s|%s", email, count, createdAt, lastOpened);
        ops.set(key, newValue, 7, TimeUnit.DAYS);

        logger.info("Updated tracking data - Key: {}, New Value: {}", key, newValue);

        return Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAEElEQVR42mP8z/C/HwAE/wJ/lB7B2QAAAABJRU5ErkJggg=="
        );
    }

    @GetMapping("/status/{trackingId}")
    public Map<String, Object> getTrackingStatus(@PathVariable String trackingId) {
        try {
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            String key = "tracking:" + trackingId;
            String data = ops.get(key);
            logger.info("Retrieved tracking status for ID {}: {}", trackingId, data);
            
            if (data == null) {
                logger.warn("No tracking data found for ID: {}", trackingId);
                return Collections.singletonMap("error", "Tracking ID not found");
            }

            String[] parts = data.split("\\|");
            logger.info("Split parts: {}", Arrays.toString(parts));

            Map<String, Object> response = new HashMap<>();
            response.put("tracking_id", trackingId);
            response.put("email", parts[0]);
            try {
                response.put("count", Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                response.put("count", 0);
                logger.warn("Invalid count format for ID {}, using 0", trackingId);
            }

            String lastOpened = parts[3];
            String createdAt = parts[2];

            response.put("last_opened", lastOpened);
            response.put("created_at", createdAt);
            return response;
        } catch (Exception e) {
            logger.error("Error getting tracking status for ID {}: {}", trackingId, e.getMessage());
            return Collections.singletonMap("error", "Error retrieving tracking status: " + e.getMessage());
        }
    }

    @GetMapping("/ping")
    public Map<String, String> healthCheck() {
        return Collections.singletonMap("message", "pong");
    }

    // To check if redis is working
    @GetMapping("/redis-test")
    public Map<String, Object> testRedis() {
        Map<String, Object> response = new HashMap<>();
        try {
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            String testKey = "test:" + UUID.randomUUID().toString();
            String testValue = "test-value-" + Instant.now().toString();
            
            // Test write
            ops.set(testKey, testValue, 1, TimeUnit.MINUTES);
            logger.info("Successfully wrote to Redis - Key: {}, Value: {}", testKey, testValue);
            
            // Test read
            String retrievedValue = ops.get(testKey);
            logger.info("Successfully read from Redis - Key: {}, Retrieved Value: {}", testKey, retrievedValue);
            
            response.put("status", "success");
            response.put("testKey", testKey);
            response.put("writtenValue", testValue);
            response.put("retrievedValue", retrievedValue);
            response.put("redisConnection", "working");
        } catch (Exception e) {
            logger.error("Redis test failed: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("redisConnection", "failed");
        }
        return response;
    }

    // To check if a certain trackingId exist in our Tracking Keys
    @GetMapping("/debug-tracking/{trackingId}")
    public Map<String, Object> debugTracking(@PathVariable String trackingId) {
        Map<String, Object> response = new HashMap<>();
        try {
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            String key = "tracking:" + trackingId;
            
            // Get all keys matching the pattern
            Set<String> allKeys = redisTemplate.keys("tracking:*");
            logger.info("All tracking keys in Redis: {}", allKeys);
            
            // Get the specific tracking data
            String data = ops.get(key);
            logger.info("Tracking data for ID {}: {}", trackingId, data);
            
            response.put("trackingId", trackingId);
            response.put("redisKey", key);
            response.put("data", data);
            response.put("allTrackingKeys", allKeys);
            response.put("redisConnection", "working");
        } catch (Exception e) {
            logger.error("Debug tracking failed: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("redisConnection", "failed");
        }
        return response;
    }
}
