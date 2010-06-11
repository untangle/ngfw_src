package com.untangle.node.cpd;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="n_cpd_passed_client", schema="settings")
@SuppressWarnings("serial")
public class PassedClient extends PassedAddress
{
}
