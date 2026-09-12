package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.result.Views;

/** Consultation de son propre compte / profil. */
public interface UserQueryUseCase {

    Views.UserView currentUser();
}
