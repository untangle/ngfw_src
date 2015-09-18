{
    "uniqueId": "policy-manager-lwvWGzNwHf",
    "category": "Policy Manager",
    "description": "The amount of traffic for each policy.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "policy_id",
    "orderDesc": false,
    "units": "bytes",
    "pieGroupColumn": "policy_id",
    "pieSumColumn": "sum(s2p_bytes + p2s_bytes)",
    "readOnly": true,
    "table": "sessions",
    "title": "Traffic By Policy",
    "type": "PIE_GRAPH"
}

