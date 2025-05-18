# Hello HTTP Changelog

All notable changes to the Hello HTTP software will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

_Changes since 1.7.3_

### Added
- On pre-flight, update environment variables according to request headers, query parameters and bodies
- A "Save Raw" button at the response viewer to save raw response body to a file

### Changed
- The "Save All" button near the response body viewer has been relocated to the upper level, as it copies the whole Request and Response rather than only response body

### Fixed
- Crash when selecting the last tab of a HTTP request then switching to a gRPC or WS request


## [1.7.3] -- 2025-04-29

_Changes since 1.7.2_

### Fixed
- Search bars in code editor, body editor and viewer should not fill the parent height
- Search bar should obtain the focus when it appears
- Text field in Project Name dialog should obtain the focus when it appears

### Optimized
- Reduced latencies brought by text field re-implementation


## [1.7.2] -- 2025-03-18

_Changes since 1.7.1_

### Added
- Textual hint on variable usage to top of Variable editors (#3)
- "Open User Manual" and "Report Bug or Request Feature" buttons on the Status Bar
- Mouse hovering variable placeholders in text fields to show a tooltip for its value (if exists)

### Changed
- The text field re-implementation has been extended to cover all text fields

### Fixed
- JSON highlighting on string values was inaccurate if it contains an escape


## [1.7.1] -- 2025-01-10

_Changes since 1.7.0_

### Changed
- After clicking "Copy All" in the response viewer, the copied text now uses the Heading 2 style (`-------`) instead of the Heading 1 style (`=======`). This facilitates embedding multiple requests and responses in the same Markdown document.

### Fixed
- Crash during project import if a file is not selected or there is an error (#2)
- Double hit the enter key in Code Editor would incorrectly add an extra blank line and reset cursor to the start of the line
- Line number of text fields incorrectly appeared and was bouncing if the line is long and partially left the screen


## [1.7.0] -- 2024-12-05

_Changes since 1.6.0_

I must be crazy -- the text fields for request body and response body have been reinvented to optimize performance and fix known issues, since I am too unhappy with what the UI framework provides. Now a 4 MB JSON can be loaded and manipulated instantly, and it is still working good beyond 100 MB with millions of lines. The text fields would be further optimized and extended to replace all text fields in next minor versions.

A critical memory usage bug has also been fixed. This can lower memory usage back to an expected level when there is no large request/response -- about 0.5 GB.

The TODO list on README is also gradually being shortened as I am fulfilling the promises.

### Added
- Example-level variables
- New API for user scripting: `fun Request<*>.getApplicableVariables(): Map<String, String>`
- Prettify button in JSON request editor. This includes GraphQL and gRPC.
- Mouse hovering variable placeholders in Body Editor to show a tooltip for its value (if exists)
- Number badges in Request Parameter Type tabs to indicate the number of active entries declared in the selected example, e.g. the number of active key-value pairs of a multipart request body declared in the selected Request Example.
- Certificates in P7B (PKCS#7) format can now be imported
- Private keys in PEM or PKCS#1 formats can now be imported, and does not limit to RSA keys anymore.
- PKCS#12 (known as p12) and PFX files can now be imported as client certificates
- [Experimental] Options to switch between hardware acceleration or software rendering. This is to work around display issues on some Windows devices.

### Changed
- The main UI font has been changed to [Comme](https://github.com/googlefonts/comme) and unified among all platforms
- The main monospace font has been changed to [Pitagon Sans Mono](https://github.com/ThePitagon/pitagon-sans-mono) and unified among all platforms
- Importing CA certificates now imports all the certificates from an input file
- "Copy as cURL command" is now **non-verbose**, i.e. without `time` and `--verbose`. There is a new option "Copy as cURL verbose command" for verbose.
- Update the label of "Copy as PowerShell Invoke-WebRequest command" to confine supporting PowerShell version 6 or above only (there is no change to the underlying logic)
- Inherited values in Request Editor are now showing at the bottom rather than the top
- Number of space characters to indent or unindent in Request Body and Payload editors are changed from 4 to 2
- Syntax highlighting would be disabled for Request/Payload text fields that exceeding 32 MB size
- JSON syntax highlighting does not tolerate syntax error now

### Removed
- Text fields and response body viewer now do not trim content over 4 MB (but other limits still apply)
- Most of debug logs

### Fixed
- Request body editor, payload body editor and response body viewer are reimplemented. This fixes many of the issues or weird behavior known in Jetpack Compose text fields.
- The entire database, including all request and response bodies, should not be retained in memory. This can take up gigabytes of unnecessary memory.
- The copy button should not overlap with the search bar in the response body viewer.
- After searching for a pattern in text editor, changing the search pattern should scroll to the first of new search results

### Optimized
- Request body editor, payload body editor and response body viewer are now able to handle bodies with a size of megabytes without significant performance issues.
- Clicking the "Send" button now never freeze for a short while.
- Computation performance of JSON syntax highlighting -- processing a 30 MB JSON takes less than 0.1s now
- Peak memory use and computation performance of JSON prettifying


## [1.7.0-beta.4] -- 2024-12-01

_Changes since 1.7.0-beta.3_

### Fixed
- The entire database, including all request and response bodies, should not be retained in memory. This can take up gigabytes of unnecessary memory.

### Optimized
- The pause immediately after firing requests before UI updates has been lowered.


## [1.7.0-beta.3] -- 2024-11-30

_Changes since 1.7.0-beta.2_

### Fixed
- Crash on JSON syntax highlighting in distribution builds


## [1.7.0-beta.2] -- 2024-11-30

_Changes since 1.7.0-beta.1_

### Changed
- The size threshold of disabling syntax highlighting in Request/Payload text fields has been raised from 1.5 MB to 32 MB
- JSON syntax highlighting does not tolerate syntax error now

### Fixed
- Crash on text deletion of lines that cover the whole current viewport
- The Request/Payload text fields' viewport should scroll to follow the cursor while typing
- After searching for a pattern in text editor, changing the search pattern should scroll to the first of new search results

### Optimized
- Computation performance of large text fields
- Computation performance of JSON syntax highlighting -- processing a 30 MB JSON takes less than 0.1s now
- Memory use of JSON syntax highlighting
- Peak memory use and computation performance of JSON prettifying


## [1.7.0-beta.1] - 2024-11-24

_Changes since 1.6.0_

**WARNING: Please [make a backup](https://sunny-chung.github.io/hello-http/features/data-import-export) before start using a pre-release version!**

I must be crazy -- the text fields for request body and response body have been reinvented to optimize performance and fix known issues, since I am too unhappy with what the UI framework provides. Now a 4 MB JSON can be loaded and manipulated instantly, and it is still working good beyond 100 MB with millions of lines. The text fields would be further optimized and extended to replace all text fields in next minor versions.

The TODO list on README is also gradually being shortened as I am fulfilling the promises.

Note: For large request/response bodies, v1.7.0 has a higher memory usage than v1.6.0 as a trade-off for fast performance. It would be improved in upcoming versions. Users with large concerns on memory usage may stay at v1.6.

### Added
- Example-level variables
- New API for user scripting: `fun Request<*>.getApplicableVariables(): Map<String, String>`
- Prettify button in JSON request editor. This includes GraphQL and gRPC.
- Mouse hovering variable placeholders in Body Editor to show a tooltip for its value (if exists)
- Number badges in Request Parameter Type tabs to indicate the number of active entries declared in the selected example, e.g. the number of active key-value pairs of a multipart request body declared in the selected Request Example.
- Certificates in P7B (PKCS#7) format can now be imported
- Private keys in PEM or PKCS#1 formats can now be imported, and does not limit to RSA keys anymore.
- PKCS#12 (known as p12) and PFX files can now be imported as client certificates
- [Experimental] Options to switch between hardware acceleration or software rendering. This is to work around display issues on some Windows devices.

### Changed
- The main UI font has been changed to [Comme](https://github.com/googlefonts/comme) and unified among all platforms
- The main monospace font has been changed to [Pitagon Sans Mono](https://github.com/ThePitagon/pitagon-sans-mono) and unified among all platforms
- Importing CA certificates now imports all the certificates from an input file
- "Copy as cURL command" is now **non-verbose**, i.e. without `time` and `--verbose`. There is a new option "Copy as cURL verbose command" for verbose.
- Update the label of "Copy as PowerShell Invoke-WebRequest command" to confine supporting PowerShell version 6 or above only (there is no change to the underlying logic)
- Inherited values in Request Editor are now showing at the bottom rather than the top
- Number of space characters to indent or unindent in Request Body and Payload editors are changed from 4 to 2
- Syntax highlighting would be disabled for Request/Payload text fields that exceeding 1.5 MB size

### Removed
- Text fields and response body viewer now do not trim content over 4 MB (but other limits still apply)
- Most of debug logs

### Fixed
- Request body editor, payload body editor and response body viewer are reimplemented. This fixes many of the issues or weird behavior known in Jetpack Compose text fields.
- The copy button should not overlap with the search bar in the response body viewer.

### Optimized
- Request body editor, payload body editor and response body viewer are now able to handle bodies with a size of megabytes without significant performance issues.
- Clicking the "Send" button now never freeze for a short while.


## [1.6.0] - 2024-07-22

_Changes since 1.5.2_

This version introduces the ability of user scripting. It is limited to pre-flight currently, and may be expanded to other areas according to user voices and demands.

Automatic tests are also implemented and added to the development pipeline. Although it is not visible to end users, it helps to discover some bugs or unexpected behaviors under different connection settings. The coverage of tests will be expanded, and Hello HTTP is stepping away from bugs. Lots of stuffs are reimplemented and optimized. New regressions might be introduced due to reimplementation. Please let me know and I will try to fix it within a short period of time.

### Added
- Pre-flight [user scripting](https://sunny-chung.github.io/hello-http/features/user-script)
- Subproject configuration
- Duplicating an Environment -- there is a new Duplicate button in the top-right corner after selecting an environment in the Environment Editor
- Duplicating a Request Example
- Request Examples dropdown list -- useful if you have lots of examples or long example names

### Changed
- A connection now terminates immediately, even if request is not completely sent, as soon as a response with an error HTTP status (e.g. 4XX, 5XX) is received.
- For request response copying, it now truncates a request body if it is over 2 MB.
- In transport logs, individual payload is truncated if it is over 512 KB. It is configurable per direction in [Subproject Configuration](https://sunny-chung.github.io/hello-http/features/subproject-configuration).
- In transport logs, the remaining HTTP/2 data payloads are truncated if total HTTP/2 data size is over 2 MB per direction. It is configurable per direction in Subproject Configuration.
- Hello HTTP now only receives HTTP responses up to 22 MB.
- The display formats of events in transport log are slightly updated. Some variables are enclosed with a square bracket.
- Now all artifacts are generated by GitHub-hosted runners.

### Fixed
- Pressing Ctrl-Enter/Cmd-Enter inside a request body textarea did not fire the current request.
- Wrong color was used to highlight empty string literals in JSON.
- In "Body"/"Query"/"Header", when there are lots of inherited values, key-value editors for "This Example" should still appear but it did not.
- There was an unexpected extra line break after a request body in the content of request response copying.
- Out of memory error when a large request body is used.
- An unfrequented error, ConcurrentModificationException, when receiving and persisting multiple payloads.
- Memory leak when more requests are sent.
- Memory leak when a connection is not completed gracefully.
- Documentation bug -- if "Default" is selected for HTTP protocol version, HTTP/1.1 instead of HTTP/2 is used for the "http" protocol.

### Optimized
- Memory usage -- now the memory usage of request body and response body is linear.
- Disk usage -- now large requests and responses are truncated before storing, so it is less likely for a subproject to grow over several hundreds of megabytes.


## [1.6.0-beta.4] - 2024-07-16

_Changes since 1.6.0-beta.3_

### Fixed
- Wrong color was used to highlight empty string literals in JSON
- "Copy All" button was missing for plain HTTP and GraphQL requests


## [1.6.0-beta.3] - 2024-07-12

_Changes since 1.6.0-beta.2_

**WARNING: Please [make a backup](https://sunny-chung.github.io/hello-http/features/data-import-export) before start using a pre-release version!**

### Fixed
- Could not send HTTP request on some systems, primary Windows


## [1.6.0-beta.2] - 2024-07-10

_Changes since 1.6.0-beta.1_

**WARNING: Please [make a backup](https://sunny-chung.github.io/hello-http/features/data-import-export) before start using a pre-release version!**

### Fixed
- Could not send any HTTP request


## [1.6.0-beta.1] - 2024-07-09

_Changes since 1.5.2_

**WARNING: Please make a backup before start using a pre-release version!**

This version introduces the ability of user scripting. It is limited to pre-flight currently, and may be expanded to other areas according to user voices and demands.

Automatic tests are also implemented and added to the development pipeline. Although it is not visible to end users, it helps to discover some bugs or unexpected behaviors under different connection settings. The coverage of tests will be expanded, and Hello HTTP is stepping away from bugs. Lots of stuffs are reimplemented and optimized. New regressions might be introduced due to reimplementation. Please let me know and I will try to fix it within a short period of time.

### Added
- Pre-flight user scripting
- Subproject configuration
- Duplicating an Environment -- there is a new Duplicate button in the top-right corner after selecting an environment in the Environment Editor
- Duplicating a Request Example
- Request Examples dropdown list -- useful if you have lots of examples or long example names

### Changed
- A connection now terminates immediately, even if request is not completely sent, as soon as a response with an error HTTP status (e.g. 4XX, 5XX) is received.
- For request response copying, it now truncates a request body if it is over 2 MB.
- In transport logs, individual payload is truncated if it is over 512 KB. It is configurable per direction in Subproject Configuration.
- In transport logs, the remaining HTTP/2 data payloads are truncated if total HTTP/2 data size is over 2 MB per direction. It is configurable per direction in Subproject Configuration.
- Hello HTTP now only receives HTTP responses up to 22 MB.
- The display formats of events in transport log are slightly updated. Some variables are enclosed with a square bracket.
- Now all artifacts are generated by GitHub-hosted runners.

### Fixed
- In "Body"/"Query"/"Header", when there are lots of inherited values, key-value editors for "This Example" should still appear but it did not.
- There was an unexpected extra line break after a request body in the content of request response copying.
- Out of memory error when a large request body is used.
- An unfrequented error, ConcurrentModificationException, when receiving and persisting multiple payloads.
- Memory leak when more requests are sent.
- Memory leak when a connection is not completed gracefully.
- Documentation bug -- if "Default" is selected for HTTP protocol version, HTTP/1.1 instead of HTTP/2 is used for the "http" protocol.

### Optimized
- Memory usage -- now the memory usage of request body and response body is linear.
- Disk usage -- now large requests and responses are truncated before storing, so it is less likely for a subproject to grow over several hundreds of megabytes.
