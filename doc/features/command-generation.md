---
title: Command Generation
---

# Command Generation

![Generate and copy commands](../copy-command.gif)

Some types of requests allow copying as shell commands. Click the dropdown menu near the "Send" or "Connect" button, and the copy buttons are there.

Supported operations are as follows:

| Request                      | Copying as (verbose) cURL commands | Copying as verbose [grpcurl](https://github.com/fullstorydev/grpcurl) commands | Copying as PowerShell Invoke-WebRequest command (for Windows pwsh.exe version 6 or above) |
|------------------------------|------------------------------------|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| HTTP / REST                  | ✔︎                                 | ✕                                                                              | ✔︎                                                                                        |
| WebSocket                    | ✕                                  | ✕                                                                              | ✕                                                                                         |
| GraphQL Query                | ✔︎                                 | ✕                                                                              | ✔︎                                                                                        |
| GraphQL Mutation             | ✔︎                                 | ✕                                                                              | ✔︎                                                                                        |
| GraphQL Subscription         | ✕                                  | ✕                                                                              | ✕                                                                                         |
| gRPC Unary                   | ✕                                  | ✔︎                                                                             | ✕                                                                                         |
| gRPC Client Streaming        | ✕                                  | ✔︎                                                                             | ✕                                                                                         |
| gRPC Server Streaming        | ✕                                  | ✔︎                                                                             | ✕                                                                                         |
| gRPC Bidirectional Streaming | ✕                                  | ✔︎                                                                             | ✕                                                                                         |
