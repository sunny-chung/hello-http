---
title: User Script
---

# User Script

Hello HTTP allows the use of custom script to perform a limited set of operations that are not possible or difficult to do via the UI. The supported programming language is [Kotlite](https://sunny-chung.github.io/kotlite/#_the_kotlite_language), a subset of the **Common** variant of the [Kotlin](https://kotlinlang.org/docs/basic-syntax.html) 1.9 **script** language. Script means it does not require a `main` function and the statements at the outermost scope will be executed sequentially.

It is not quite possible to do anything harmful by user scripts, because both the language and the APIs available to be used are limited. For example, it is not possible to make another HTTP request directly from the script, nor modify the URL of the current script, nor execute system commands. There is also a short time limit on execution.

An example use case of user scripts is to generate some hash or signature with some encryption keys and embed into a request before sending it.

## Providing custom files for user script use

Files can be imported into an [Environment](environments) to be consumed by user script. Only small files can be imported.

![User Files](../user-files.png)

Names can be changed after importing.

## Pre-flight Script

Custom pre-flight script can be provided to a [request example](request-examples-and-payload-examples). It would be executed before firing the request.

It is only available for:
- Plain/RESTful HTTP requests
- GraphQL requests

Similar to other request example properties, the script in the "Base" example can be overridden by other request examples.

![Pre-flight Script](../pre-flight-script.png)

The only supported mutation operations are:
- append headers
- append query parameters

All APIs in the [standard library](https://sunny-chung.github.io/kotlite/#_built_in_and_standard_library_apis) can be used.

In additional, below inputs and APIs are available as follow.

### Global Properties

```kotlin
/**
 * Request object.
 * 
 * @param <BODY> One of `Nothing`, `StringBodyClass`, `FormUrlEncodedBodyClass`, `MultipartBodyClass`, `FileBodyClass`, `GraphqlBodyClass`.
 */
val request: MutableRequest<BODY>

/**
 * The environment chosen to be active. Null if there is none.
 */
val environment: Environment?
```

### Types

#### `Request`

```kotlin
class Request<BODY>

val Request<*>.url: String
val Request<*>.method: String
val Request<*>.headers: List<Pair<String, String>>
val Request<*>.queryParameters: List<Pair<String, String>>
val <BODY> Request<BODY>.body: BODY?

/**
 * Get a properly encoded URI embedded with query parameters.
 */
fun Request<*>.getResolvedUri(): String
```

#### `MutableRequest`

```kotlin
class MutableRequest<BODY> : Request<BODY>

fun MutableRequest<*>.addHeader(key: String, value: String)
fun MutableRequest<*>.addQueryParameter(key: String, value: String)
```

#### `Environment`

```kotlin
class Environment

val Environment.name: String

/**
 * Environment variables. Only enabled variables are available.
 */
val Environment.variables: List<UserKeyValuePair>

/**
 * User files. Only enabled user files are available.
 */
val Environment.userFiles: List<ImportedFile>
```

#### `UserKeyValuePair`

```kotlin
class UserKeyValuePair

val UserKeyValuePair.key: String
val UserKeyValuePair.value: String

/**
 * Either "String" or "File".
 */
val UserKeyValuePair.valueType: String

/**
 * If valueType is "String", return the bytes of the String value.
 * If valueType is "File", return the bytes of the underlying file, or null if no file is selected. Exception would be thrown if a file is specified and cannot be read.
 */
fun UserKeyValuePair.readValueBytes(): ByteArray?
```

#### `StringBody`

```kotlin
class StringBody

val StringBody.value: String
```

#### `FormUrlEncodedBody`

```kotlin
class FormUrlEncodedBody

val FormUrlEncodedBody.value: List<UserKeyValuePair>
```

#### `MultipartBody`

```kotlin
class MultipartBody

val MultipartBody.value: List<UserKeyValuePair>
```

#### `FileBody`

```kotlin
class FileBody

val FileBody.filePath: String?

/**
 * Return the bytes of the underlying file, or null if no file is selected. Exception would be thrown if a file is specified and cannot be read.
 */
val FileBody.readBytes(): ByteArray?
```

#### `GraphqlBody`

```kotlin
class GraphqlBody

val GraphqlBody.document: String
val GraphqlBody.variables: String
val GraphqlBody.operationName: String?
```

#### `ImportedFile`

```kotlin
class ImportedFile

val ImportedFile.name: String
val ImportedFile.originalFilename: String
val ImportedFile.createdWhen: KInstant
val ImportedFile.content: ByteArray
```

#### `PublicKey`

```kotlin
class PublicKey
```

#### `PrivateKey`

```kotlin
class PrivateKey
```

#### `SecretKey`

```kotlin
class SecretKey
```

### Extensions

#### Encoding

````kotlin
fun ByteArray.encodeToBase64String(): String
fun ByteArray.encodeToBase64UrlString(): String
fun ByteArray.encodeToHexString(): String

fun String.decodeBase64StringToByteArray(): ByteArray
fun String.decodeBase64UrlStringToByteArray(): ByteArray
fun String.decodeHexStringToByteArray(): ByteArray

fun String.decodeJsonStringToMap(): Map<Any?, Any?>
````

#### Crypto

````kotlin
fun ByteArray.toSha1Hash(): ByteArray
fun ByteArray.toSha256Hash(): ByteArray
fun ByteArray.toSha512Hash(): ByteArray
fun ByteArray.toMd5Hash(): ByteArray
fun ByteArray.toSha1WithRsaSignature(rsaPrivateKey: PrivateKey): ByteArray
fun ByteArray.toSha256WithRsaSignature(rsaPrivateKey: PrivateKey): ByteArray

fun ByteArray.toPkcs8RsaPublicKey(): PublicKey
fun ByteArray.toPkcs8RsaPrivateKey(): PrivateKey
fun ByteArray.toAesSecretKey(): SecretKey

/**
 * Example value of parameter `algorithm`: "AES/CBC/PKCS5Padding"
 */
fun ByteArray.asEncrypted(algorithm: String, key: SecretKey, iv: ByteArray? = null): ByteArray
fun ByteArray.asDecrypted(algorithm: String, key: SecretKey, iv: ByteArray? = null): ByteArray
````
