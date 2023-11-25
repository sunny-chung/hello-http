---
title: Project and Request Management
---

# Project and Request Management

## Structure
![Sidemenu](../sidemenu.png)

- 0..n Projects, where each has
  - 0..n Subprojects, where each has
    - 0..n [Environments](environments)
    - 0..n Requests and Folders
      - Requests can be nested in Folders with infinite number of levels

In a microservice architecture, each Subproject may represent a microservice.

For frontend developers, the main server may be a Subproject. An endpoint of a third-party integration, e.g. S3, may be
another Subproject.

## Creating Projects
When there is no Project or Subproject, there will be a big button to let you create one.

If there is already a Project or Subproject, click "+" button to create another one.

## Selecting a Project / Subproject
On launching the application, a Project and Subproject needs to be selected first to proceed. Click on an existing
Project / Subproject to select it.

If you have just followed the previous step to create a Project / Subproject, it would be automatically selected.

After that, if you would like to change to another Project / Subproject, click the drop down menu to change.

## Creating Requests and Folders
There are two ways to create a Request or Folder.
- Click the "+" button right to the search box, then select the type. This would create at the outermost layer.
- Right-click a Folder, then select the intended action. This would create inside that Folder.

## Renaming a Request / Folder
Just double-click the Request / Folder, make changes and hit enter.

## Reordering and Moving into/out of Folders
![Drag to reorder](../drag-to-reorder.gif)
Nothing more straight-forward than dragging and dropping!

## Searching
![Search](../search.gif)
Type something in the search box. Everything can be found even if they are deeply buried!
