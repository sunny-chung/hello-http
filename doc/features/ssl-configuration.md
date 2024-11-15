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
- PEM (also known as CER or CRT)
- P7B

The formats are detected automatically.

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
- PEM (also known as CER or CRT) containing only one certificate
- P7B containing only one certificate

Accepted formats for a private key are:
- Unencrypted PKCS #8 DER
- Password-encrypted PKCS #8 DER
- PKCS #1 DER
- Unencrypted PKCS #8 PEM
- Password-encrypted PKCS #8 PEM
- PKCS #1 PEM

Alternatively, a PKCS#12 or P12 or PFX bundle containing exactly one certificate and one private key can be imported. If multiple entries of the same kind are found, only the first one would be imported.

The formats are detected automatically. Files with the `.key` file extension are usually in PEM or DER formats.


## Disable SSL Verification

SSL certificate verification can be disabled. By default, SSL verification is enabled.

