{
    "uniqueId": "intrusion-prevention-oBzMcymv",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions detected grouped by destination IP address.",
    "displayOrder": 701,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "dest_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Destination IP Addresses (logged)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
