---
title: Viewing Responses
---

# Viewing Responses

## Connection Security

![Connection Security](../connection-security.gif)

From v1.4.3 onwards, the connection security level and certificates used would be displayed in the top-right corner of
the response viewer.

The possible indicator icons are described below.

| Icon                                                   | Name           | Description                                                                                                                                                                  |
|--------------------------------------------------------|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![Not encrypted](../conn-security-unencrypted.png)     | Not encrypted  | The connection is not encrypted. Anybody with access to the wire is able to intercept and manipulate the traffic.                                                            |
| ![Unverified TLS](../conn-security-unverified-tls.png) | Unverified TLS | The connection is encrypted, but the certificates are not verified. It is vulnerable to [man-in-the-middle attacks](https://en.wikipedia.org/wiki/Man-in-the-middle_attack). |
| ![One-way TLS](../conn-security-tls.png)               | One-way TLS    | The connection is encrypted, and the server certificates are trusted according to the environment SSL setting.                                                               |
| ![mTLS](../conn-security-mtls.png)                     | mTLS           | The connection is encrypted, and the client certificate and the server certificates are mutually trusted.                                                                    |

More details of the established TLS connection are available in the [Transport Timeline](transport-timeline).

## Searching

Click the response, then press Ctrl-F or Command-F to bring up a search bar.

![Searching in Response](../response-searching.png)

## Filtering JSON by JSON Path

If a response is recognized as JSON (according to the `Content-Type` header), a text box would appear at the bottom to
allow entry of a JSON path to filter the response. The supported expressions are
documented [here](https://github.com/json-path/JsonPath).

![Filtering JSON Response](../response-json-filtering.gif)

## Collapsing/Expanding JSON

JSON arrays and objects can be collapsed and expanded.

![Collapse/Expand JSON Data Structures](../collapse-json.gif)

## Copying Responses

Hover any copiable fields and click the "copy" button to copy. A prompt is displayed if copying is successful.

![Copying Responses](../copy-response.gif)

