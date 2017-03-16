{
    "uniqueId": "configuration-backup-eN8Ot9wh",
    "category": "Configuration Backup",
    "description": "A summary of configuration backup actions.",
    "displayOrder": 20,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "count(*) as total",
        "sum(case when success is true then 1 else 0 end) as passed",
        "sum(case when success is false then 1 else 0 end) as failed"
    ],
    "textString": "Configuration backup attempted {0} backups where {1} succeeded and {2} failed.", 
    "readOnly": true,
    "table": "configuration_backup_events",
    "title": "Configuration Backup Summary",
    "type": "TEXT"
}
