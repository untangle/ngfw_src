{
    "category": "Hosts",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "value",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "like",
            "value": "%penalty-box%"
        }
    ],
    "defaultColumns": ["time_stamp","address","key","value","old_value"],
    "description": "Shows when hosts are tagged with penalty-box or have the tag removed.",
    "displayOrder": 1012,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "host_table_updates",
    "title": "Penalty Box Events",
    "uniqueId": "host-viewer-Kg7KXElMd7"
}
