package com.untangle.node.cpd;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="n_cpd_passed_server", schema="settings")
@SuppressWarnings("serial")
public class PassedServer extends PassedAddress
{
}
