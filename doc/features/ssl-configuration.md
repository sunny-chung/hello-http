---
title: SSL Configuration
---

# SSL Configuration

![SSL setting](../ssl-config.png)

SSL configuration is per Environment and resides in the [Environment setting](environments).

Note that imported certificates and private keys **will be exported** along with Hello HTTP data dumps and backups, even
if they are disabled. If you don't want them to be exported when sharing a project to the others, remove them before
doing exports.

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

## Disable System CA Certificates

It is possible to disable system CA certificates, so that only your custom certificates are trusted.
By default, it is not disabled.

## Client Certificate

At most one client certificate and private key pair can be imported per environment. This is mandatory for
[mTLS connections](https://en.wikipedia.org/wiki/Mutual_authentication#mTLS).

Accepted formats for a client certificate are:
- DER
- PEM containing only one certificate

Accepted formats for a private key are:
- Unencrypted PKCS #8 DER
- Password-encrypted PKCS #8 DER

An unencrypted PKCS #8 DER key file can be converted from a PEM file using OpenSSL.
```
openssl pkcs8 -topk8 -in clientKey.pem -out clientKey.pkcs8.der -outform DER -nocrypt
```

A password-encrypted PKCS #8 DER key file can be converted from a PEM file using OpenSSL.
```
openssl pkcs8 -topk8 -in clientKey.pem -out clientKey.pkcs8.encrypted.der -outform DER
```


## Disable SSL Verification

SSL certificate verification can be disabled. By default, SSL verification is enabled.

