---
title: Data Import & Export
---

# Data Import & Export

## Data Import

Welcome to bring in data from other applications to try Hello HTTP! Enjoy data freedom! Currently suppported data
formats are:

- Insomnia v4 JSON
- Postman v2 data dump ZIP
- Postman v2 single collection JSON
- Hello HTTP data dump

## Data Export
Create a manual backup, or leave anytime you want! Currently suppported data formats are:
- Insomnia v4 JSON
- Hello HTTP data dump
- Postman v2 Data Dump (One File per Project or Environment)

Any number of projects can be selected to export at a time.

## Feature Matrix

This feature is limited by what the software supports, and the structural differences between Hello HTTP and them. For
example, Postman supports WebSocket out-of-the-box, but these requests cannot be exported or imported. Below summarises
limitation of all the options.

Hello HTTP data dump exports or imports everything supported, so it is not on the list.

| Item                                  | Import from Insomnia               | Export to Insomnia                 | Import from Postman | Export to Postman                   |
|---------------------------------------|------------------------------------|------------------------------------|---------------------|-------------------------------------|
| Request folder & Subproject structure | ✔︎                                 | ✔︎                                 | ✔︎                  | Subprojects are exported as folders |
| HTTP & REST requests                  | ✔︎                                 | ✔︎                                 | ✔︎                  | ✔︎                                  |
| Request Documentation                 | N/A                                | N/A                                | N/A                 | N/A                                 |
| WebSocket                             | ✔︎                                 | Only the first payload is exported | ✕                   | ✕                                   |
| GraphQL queries & mutations           | ✔︎                                 | ✔︎                                 | TODO                | Operation name is not exported      |
| GraphQL subscriptions                 | ✔︎                                 | Only the first payload is exported | ✕                   | ✕                                   |
| gRPC (via Reflection)                 | Service Definition is not imported | Service Definition is not exported | ✕                   | ✕                                   |
| gRPC (via `.proto` files)             | N/A                                | N/A                                | ✕                   | ✕                                   |

`N/A` means that feature is not available in Hello HTTP.

