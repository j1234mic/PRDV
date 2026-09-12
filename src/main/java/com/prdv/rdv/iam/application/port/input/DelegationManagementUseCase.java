package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/** Delegation temporaire et revoquable de permissions entre utilisateurs. */
public interface DelegationManagementUseCase {

    Views.DelegationView grant(AdminCommands.GrantDelegation command);

    List<Views.DelegationView> delegationsForMe();

    void revoke(Long delegationId);
}
