package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Donnees pour les analystes : seules des vues anonymisees / pseudonymisees
 * sont exposees (anonymisation pour analytics).
 */
public interface AnalyticsQueryUseCase {

    List<Views.AnonymizedUserView> anonymizedUsers();
}
