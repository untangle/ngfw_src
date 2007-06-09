-- events conversion for release-5.0

ALTER TABLE events.mvvm_login_evt RENAME TO u_login_evt;
ALTER TABLE events.transform_state_change RENAME TO u_node_state_change;
ALTER TABLE events.pl_endp RENAME TO pl_endp;
ALTER TABLE events.pl_stats RENAME TO pl_stats;
ALTER TABLE events.shield_rejection_evt RENAME TO n_shield_rejection_evt;
ALTER TABLE events.shield_statistic_evt RENAME TO n_shield_statistic_evt;
ALTER TABLE events.portal_login_evt RENAME TO n_portal_login_evt;
ALTER TABLE events.portal_logout_evt RENAME TO n_portal_logout_evt;
ALTER TABLE events.portal_app_launch_evt RENAME TO n_portal_app_launch_evt;
ALTER TABLE events.mvvm_lookup_evt RENAME TO u_lookup_evt;
