---
title: API Documentation Generation
---

# API Documentation Generation

## Write Description

You may write description per API or per example level. By default, descriptions of examples inherit from the base example, but it could be overridden in individual examples.

The description can be written in **Markdown** format. Different from the standard Markdown syntax, it also supports tables, images, strikethrough and character-level formatting, etc.. But image support is only limited to base-64 data images, and images with URLs or file paths are not supported due to privacy and security concerns.

![Write Description](../apidoc-write.png)

## Generate API Documentation

*(Experimental since v1.9.0)*

The descriptions, requests and responses of a subproject along with environments and metadata could be exported as an API documentation, by clicking the menu button next to the subproject selector and then the "Export as API Doc in HTML" context menu item.

![Generate API Documentation](../apidoc-entrance.png)

Currently, the output is a single *self-contained* HTML file that can be viewed **offline**. Different views can be toggled from the side menu. An example of the output is as follows:

![Sample API Documentation](../apidoc-output.png)

It is **printable** and **convertible** to PDF. 

![Print API Documentation](../apidoc-printable.png)
