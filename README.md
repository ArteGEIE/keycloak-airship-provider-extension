# Keycloak Airship Provider Extension

This extension allows Keycloak to send emails using the Airship email service. It implements a custom `EmailSenderProvider` that integrates with Airship's API.

## Overview

The Keycloak Airship Provider Extension enables sending all Keycloak emails (password reset, verification, etc.) through Airship's email delivery service instead of using the default SMTP email provider.

## Configuration

### Environment Variables

The extension uses the following environment variables:

- `AIRSHIP_APP_KEY` (required): The application key used to authenticate with Airship
- `AIRSHIP_ACCESS_TOKEN` (required): The access token used to authenticate with Airship
- `AIRSHIP_DOMAIN` (optional): The Airship API domain to use (defaults to `https://go.airship.eu`)
- `AIRSHIP_HEADER` (optional): The Airship API header (defaults to `vnd.urbanairship+json`)
- `AIRSHIP_EMAIL_SENDER` (optional): The email address used as the sender for emails, 
   if not set, it will use the default email sender from Keycloak

- `AIRSHIP_EMAIL_SENDER_NAME` (optional): The name used as the sender for emails,
   if not set, it will use the default email sender name from Keycloak

- `AIRSHIP_EMAIL_REPLY_TO` (optional): The email name and address used as the reply-to for emails, 
   if not set, it will use the default email reply-to and reply-to name from Keycloak ()
   (example: `John Doe <john.doe@example.com>`)

- `AIRSHIP_ENDPOINT` (required): The Airship API endpoint for sending emails (defaults to `/api/create-and-send`)
- `KEYCLOAK_EMAIL_PROVIDER_PRIORITY` (optional): The priority of the email provider (defaults to `100`)

### Installing the Extension

**Important:** The Airship provider will automatically take precedence over the default SMTP provider due to its higher priority. No manual configuration is required in the realm settings. If you need to disable the Airship provider and use the default SMTP provider, set the environment variable `KEYCLOAK_EMAIL_PROVIDER_PRIORITY` to a value lower than 100.

1. Build the extension with Maven:
   ```
   mvn clean package -DskipTests
   ```

2. Copy the resulting JAR file to Keycloak's `providers` directory:
   ```
   cp target/airship-provider-0.0.1-SNAPSHOT.jar /path/to/keycloak/providers/
   ```

3. Set the required environment variables.

4. Restart Keycloak to load the provider.

## Features

- Force sending all Keycloak emails through Airship's API
- Uses Airship's "Send" endpoint to send transactional emails
- Configurable sender information
- Adds "keycloak" category tag to all emails for easy filtering in Airship

## Development

### Project Structure

- `AirshipEmailFactory`: Factory class that creates instances of `AirshipEmailProvider`
- `AirshipEmailProvider`: Implementation of Keycloak's `EmailSenderProvider` interface

### Building from Source

```
   mvn clean package -DskipTests
```

### Tests

```
mvn clean test
```

## References

For more information about Airship's API, see the [official documentation](https://docs.airship.com/api/ua/?openapi=http#). 
