---
title: SSL Configuration
---

# SSL Configuration

![SSL setting](../ssl-config.png)

## Additional Trusted CA Certificates

Additional to the default trusted CA certificates, custom ones can be imported to verify the server identities.
Currently, the accepted formats are:
- DER
- PEM containing only one certificate

A DER certificate can be converted from a PEM certificate using OpenSSL:
```
openssl x509 -in cert.pem -out cert.der -outform DER
```

Imported certificates can be disabled by unchecking the corresponding green tick box, or deleted. Changes to the
original file would not affect imported ones.

Self-signed certificates are also supported at this moment. A warning will be displayed if a non-CA certificate is
imported.

## Disable SSL Verification

SSL certificate verification can be disabled in the [Environment setting](environments).

By default, SSL verification is enabled.

