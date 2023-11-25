---
title: Automatic Backups
---

# Automatic Backups

No more frustrations due to misclicking delete buttons and mistakes! An automatic backup of **all projects** is made at
- every launch of Hello HTTP
- every 6 hours while Hello HTTP is running

![Backups](../backup.png)

## Retention Period

These backups are only kept for 15 days by default. This retention period is configurable.

Setting it to 0 days would disable automatic backups.

Setting it to a big number, e.g. 999999, would keep all the backups. It can demand a large disk space.

## Backup Destination
Open the Setting & Data dialog, in the "Data" tab, scroll to the "Automatic Backup" section, then
click "Open Backup Directory". This is where these backups are placed.

## Restoring a Backup
Open the Setting & Data dialog, in the "Data" tab, in the "Import Projects" section, choose the desired backup file,
select "Hello HTTP Data Dump" as the format, then click "Import". This restores all the projects as new projects.

Existing projects have to be deleted manually after restoration. Alternatively, you can move the backups to another
location, then empty Hello HTTP's data directory before restoration.
