---
title: gRPC
---

# gRPC

All four types of gRPC service methods are supported:
- Unary RPC
- Server streaming RPC
- Client streaming RPC
- Bidirectional streaming RPC

gRPC requires a service definition, as known as API specification in Hello HTTP, is available before invoking RPC calls.
Hello HTTP supports downloading the specification via the Server Reflection extension. This requires the gRPC server to
[enable this feature](https://github.com/grpc/grpc/blob/master/doc/server_reflection_tutorial.md). Once downloaded, it can be used to call other gRPC servers operating with the same API
specification within the same Hello HTTP Subproject.

Reading the API specification from .proto files is not yet supported.

Below demonstrates how to download the API specification, share among a unary call and a bi-directional streaming gRPC
call, and invoke them.

![gRPC Demo](../grpc.gif)

## Using Plaintext or TLS protocols
To use the plaintext protocol, use `http://` or `grpc://` schemes in the URL.

To use the TLS protocol, use `https://` or `grpcs://` schemes in the URL.

SSL setting can be configured in the [Environment setting](../features/environments).

## Payload Data Format
End users input **JSON** payloads and read **JSON** responses.
JSON data is converted to Protobuf for data transmission transparently.

## Updating an API Specification
On clicking the download schema button and the schema is successfully retrieved, it would **replace the one that has the
name `{host}:{port}`** in the same Subproject. If there is none, even if a schema is selected, Hello HTTP would create a
new one with this name. The name can be changed afterwards, and can be managed, as described below.

## Managing API Specifications
Click the pencil (Edit) button next to the current Subproject name. gRPC API specifications can be managed in the
"Edit Subproject" dialog.

![Manage gRPC API Specifications](../manage-grpc-apispec.gif)

## Copy as `grpcurl` commands
A [grpcurl](https://github.com/fullstorydev/grpcurl) command can be copied to send the current request in a command
shell. All service method types are supported. If only response bodies are needed, the verbose option `-v` can be
removed.

Note: It is only available for Linux & macOS.

![grpcurl](../grpcurl.png)
