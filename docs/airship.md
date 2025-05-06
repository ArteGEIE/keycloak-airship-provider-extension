AirShip
===

AirShip is a solution to send Emails through an API.

This document summarizes useful information for Airship integration. For more information, see the official Airship documentation at https://docs.airship.com/api/ua/?openapi=http#

# Vocabulary

- `channel`: an identified contact, that could be related to an email, a device, etc.
- `push object`: any notification (could be email, push notification, etc), it includes the push details and the audience


# Authenticating
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

# Send Emails

To send an email, you need to create a channel and then send a transactional email. It is possible to do it in one request.

## Create a channel and send a transactional email

See https://docs.airship.com/api/ua/?openapi=http#operation-api-channels-email-post for more information.
This option could be a good option if you want to send an email to a user that is not registered in Airship yet.

```shell
curl --request POST \
  --url https://go.airship.eu/api/create-and-send \
  --header 'Accept: application/vnd.urbanairship+json; version=3' \
  --header 'Authorization: Bearer {{airship_token}}' \
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
      "reply_to": "sender@domain.com"
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
 * `message_type`: the type of the message. Could be `transactional` or `commercial`. For this provider, we should always use `transactional`.
 * `campaigns.categories`: optional, specify tags concerning the email. For keycloak integration, we can set `"categories": ["keycloak"]` and add a tag with email type (reset password, etc). This will help us to filter the emails in Airship.

# Configuration

We should be able to configure these parameters with environment variables:
 * AIRSHIP_API_KEY: API key used to authenticate the API calls
 * AIRSHIP_DOMAIN: the domain used to send the emails. It should be `go.airship.eu` for Europe and `go.airship.com` for US