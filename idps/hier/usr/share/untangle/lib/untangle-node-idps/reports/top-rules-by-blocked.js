{
    "uniqueId": "intrusion-prevention-56KUY1Et",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions blocked by rule.",
    "displayOrder": 202,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "msg",
    "pieSumColumn": "sum(blocked::int)",
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Rules (blocked)",
    "type": "PIE_GRAPH"
}
