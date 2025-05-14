# Email Tracking Service

A Java-based service for sending and tracking emails using Redis for storage and Spring Boot for the web framework.

## Features

- Send emails to multiple recipients (TO, CC, BCC)
- Track email opens using pixel tracking
- Custom HTML templates with variable substitution
- Redis-based tracking storage
- Configurable tracking expiration
- RESTful API endpoints
- Comprehensive email tracking analytics

## Setup

### Prerequisites

- Java 17 or higher
- Maven
- Redis server
- SMTP server access

### Environment Variables

```bash
# SMTP Configuration
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=your-username
SMTP_PASSWORD=your-password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Application Configuration
TRACKING_DOMAIN=https://your-domain.com
TRACKING_ID_EXPIRATION=86400  # 1 day in seconds
SERVER_PORT=8080  # Optional, defaults to 8080
```

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/email-tracking.git
cd email-tracking
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
cd demo
mvn spring-boot:run
```

## API Endpoints

### 1. Send Email

`POST /email/send`

Sends emails to multiple recipients with optional tracking.

#### Request Body:

```json
{
  "recipients": {
    "receivers": [
      {
        "email": "test@example.com",
        "tracking_id": "",
        "want_to_track": true,
        "type": "to"  // "to", "cc", or "bcc"
      }
    ],
    "from": "sender@yourdomain.com"
  },
  "email_body": {
    "html_template": "<!DOCTYPE html><html><body>Hello {{ name }}!</body></html>",
    "subject": "Test Email",
    "parameters": {
      "test@example.com": {
        "name": "John Doe"
      }
    }
  }
}
```

#### Response:

```json
{
  "status": {
    "test@example.com": "Success:tracking_id:123"
  }
}
```

### 2. Track Email Opens

`GET /email/pixel/{tracking_id}`

Endpoint for the tracking pixel. Returns a 1x1 transparent PNG and logs the email open.

#### Response:
- Returns a transparent 1x1 pixel image
- Status: 200 OK if tracking ID exists, 404 if not found

### 3. Check Tracking Status

`GET /email/status/{tracking_id}`

Get the current status of an email tracking ID.

#### Response:

```json
{
  "tracking_id": "123",
  "email": "test@example.com",
  "count": 2,
  "last_opened": "2024-01-18T15:04:05Z",
  "created_at": "2024-01-18T10:00:00Z"
}
```

### 4. Redis Status

`GET /email/redis-test`

Simple health check endpoint to test if redis is working.

#### Response:

```json
{
    "retrievedValue": "test-value-2025-05-14T06:11:55.893005400Z",
    "writtenValue": "test-value-2025-05-14T06:11:55.893005400Z",
    "redisConnection": "working",
    "testKey": "test:305eed64-54b5-4323-a859-f8627108850c",
    "status": "success"
}
```

### 5. Key Check

`GET /email/debug-tracking/{tracking_id}`

Check if a key is present in our tracking keys

#### Response:

```json
{
    "data": "sender@yourdomain.com:10:2025-05-13T14:08:01.334190800Z:07:38.457485Z:2025-05-13T14:07:38.457485Z",
    "redisKey": "tracking:7f446947-7733-47a1-bc01-376cd84a765f",
    "redisConnection": "working",
    "allTrackingKeys": [
        "tracking:9b87c0e1-2498-4129-91da-2bc09d6a6970",
        "tracking:7f446947-7733-47a1-bc01-376cd84a765f"
    ],
    "trackingId": "7f446947-7733-47a1-bc01-376cd84a765f"
}
```

### 6. Health Check

`GET /email/ping`

Simple health check endpoint.

#### Response:

```json
{
  "message": "pong"
}
```


## Template Variables

The HTML template supports variable substitution using the `{{ variable_name }}` syntax. Variables are defined per recipient in the `parameters` field of the request.

## Error Handling

- All endpoints return appropriate HTTP status codes
- Detailed error messages are included in the response
- Tracking IDs expire after the configured duration (default: 1 day)
- Comprehensive logging for debugging and monitoring

## Examples

### Sending a Test Email

```bash
curl -X POST http://localhost:8080/api/v1/emails/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipients": {
      "receivers": [
        {
          "email": "test@example.com",
          "tracking_id": "",
          "want_to_track": true,
          "type": "to"
        }
      ],
      "from": "sender@yourdomain.com"
    },
    "email_body": {
      "html_template": "<!DOCTYPE html><html><body><h1>Hello {{ name }}!</h1></body></html>",
      "subject": "Test Email",
      "parameters": {
        "test@example.com": {
          "name": "John Doe"
        }
      }
    }
  }'
```

### Checking Email Status

```bash
curl http://localhost:8080/email/status/your-tracking-id
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support, please open an issue in the GitHub repository or contact the maintainers.
