# Keycloak Airship Provider Extension

This extension allows Keycloak to send emails using the Airship email service. It will automatically take precedence over the default SMTP provider due to its higher priority. 

In the realm settings, from, sender name, reply to and reply to name will be used as default values.

**Important:** If you need to disable the Airship provider, use the email sender provider environment variable `KC_SPI_EMAIL_SENDER_PROVIDER` with value `default`, see https://www.keycloak.org/server/configuration-provider#_configuring_a_default_provider_for_an_spi

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

### Installing the Extension

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

### AirShip implementation

We describe here how the extension works with Airship's API to send emails.

#### Vocabulary

- `channel`: an identified contact, that could be related to an email, a device, etc.
- `push object`: any notification (could be email, push notification, etc.), it includes the push details and the audience


#### Authenticating

To authenticate your application to make API calls, you should create a token for it:
- Select the Airship project
- Go to Settings
- Tokens > Manage
- Create Token
   - Add an identifier to the created token
   - Choose a role
- Copy / Paste the API Key and Access Token

To authenticate you API calls to Airship, use the `Authorization: Bearer <AccessToken>` header. See https://docs.airship.com/api/ua/?openapi=http#security  
As our Airship projects are located in Europe, we should use https://go.airship.eu as base URL. See https://docs.airship.com/api/ua/?openapi=http#servers

#### Send Emails

To send an email, you need to create a channel and then send a transactional email. It is possible to do it in one request.

##### Create a channel and send a transactional email

See https://docs.airship.com/api/ua/?openapi=http#operation-api-channels-email-post for more information.
This option could be a good option if you want to email a user that is not registered in Airship yet.

```shell
curl --request POST \
  --url https://go.airship.eu/api/create-and-send \
  --header 'Accept: application/vnd.urbanairship+json; version=3' \
  --header 'Authorization: Bearer <AccessToken>' \
  --header 'Content-Type: application/json' \
  --data '{
  "audience": {
    "create_and_send": [
      {
        "ua_address": "user1@domain.com",
      }
    ]
  },
  "device_types": [
    "email"
  ],
  "notification": {
    "email": {
      "subject": "Hello from Airship",
      "html_body": "<h1>Hi!</h1><p>A great email content</p>",
      "plaintext_body": "Hi A great email content",
      "message_type": "transactional",
      "sender_name": "Sender Name",
      "sender_address": "sender@domain.com",
      "reply_to": "sender@domain.com",
      "bypass_opt_in_level": true,
      "click_tracking": false,
      "open_tracking": false
    }
  },
  "campaigns": {
    "categories": [
      "category1",
      "category2"
    ]
  }
}'
```

* `ua_address`: the email address of the user
* `sender_name`: the name of the sender. Could be retrieved from Keycloak configuration (Realm Settings > Email > From Display Name)
* `sender_address`: the email address of the sender. Could be retrieved from Keycloak configuration (Realm Settings > Email > From)
* `reply_to`: the email address of the sender. Could be retrieved from Keycloak configuration (Realm Settings > Email > Reply-To)
* `bypass_opt_in_level`: set to `true` to bypass the opt-in level. We always want to send keycloak email even if the user has not opted in to receive transactional emails.
* `click_tracking`: set to `false` to disable click tracking.
* `open_tracking`: set to `false` to disable open tracking.
* `message_type`: the type of the message. Could be `transactional` or `commercial`. For this provider, we should always use `transactional`.
* `campaigns.categories`: optional, specify tags concerning the email. For keycloak integration, we can set `"categories": ["keycloak"]` and add a tag with email type (reset password, ...). This will help us to filter the emails in Airship.


## References

For more information about Airship's API, see the [official documentation](https://docs.airship.com/api/ua/?openapi=http#). 

## Credits

This extension was developed by [Liksi](https://www.liksi.fr/) on behalf of the Arte GEIE team from Strasbourg, France. We are open to external contributions and suggestions so feel free to create an issue or a pull request.

## License

This extension, just like Keycloak,  is licensed under the Apache License, Version 2.0. You can find the full license text in the [LICENSE](LICENSE.txt) file.