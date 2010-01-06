package com.untangle.node.cpd;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="n_cpd_passed_client", schema="settings")
public class PassedClient extends PassedAddress
{
    private static final long serialVersionUID = 1010754694943423080L;
}
