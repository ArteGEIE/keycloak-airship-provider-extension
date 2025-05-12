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
- `AIRSHIP_EMAIL_SENDER` (required): The email address used as the sender for emails
- `AIRSHIP_ENDPOINT` (required): The Airship API endpoint for sending emails (defaults to `/api/create-and-send`)

### Installing the Extension

1. Build the extension with Maven:
   ```
   mvn clean package
   ```

2. Copy the resulting JAR file to Keycloak's `providers` directory:
   ```
   cp target/airship-provider-0.0.1-SNAPSHOT.jar /path/to/keycloak/providers/
   ```

3. Set the required environment variables.

4. Restart Keycloak to load the provider.

5. Configure your realm to use the Airship provider by setting the `provider` property to `keycloak-airship-provider` in the Email settings.

## Features

- Sends all Keycloak emails through Airship's API
- Uses Airship's "Create and Send" endpoint to efficiently create email channels and send transactional emails
- Configurable sender information
- Adds "keycloak" category tag to all emails for easy filtering in Airship

## Development

### Project Structure

- `AirshipEmailFactory`: Factory class that creates instances of `AirshipEmailProvider`
- `AirshipEmailProvider`: Implementation of Keycloak's `EmailSenderProvider` interface

### Building from Source

```
mvn clean package
```

## References

For more information about Airship's API, see the [official documentation](https://docs.airship.com/api/ua/?openapi=http#). 
