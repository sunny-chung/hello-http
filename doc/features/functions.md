---
title: Functions
---

# Functions

There are a few built-in functions can be used anywhere in the Request. Functions are similar to
[environment variables](environments), but they usually generate different values for each execution.

| Function           | Description                           | Example Outputs                        |
|--------------------|---------------------------------------|----------------------------------------|
| `uuid`             | UUID v4.                              | `2b76aaa0-f960-4429-a94b-e9fcfed2ded8` |
| `now.iso8601`      | Current date-time in ISO-8601 format. | `2023-11-14T22:18:15Z`                 |
| `now.epochMills`   | Current timestamp in milliseconds.    | `1700000295256`                        |
| `now.epochSeconds` | Current timestamp in seconds.         | `1700000295`                           |

To use it, type `$((function-name))`. For example, `$((uuid))`.
