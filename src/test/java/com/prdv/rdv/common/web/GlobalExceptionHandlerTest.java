package com.prdv.rdv.common.web;

import com.prdv.rdv.planning.domain.PlanningException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Un 500 {@code INTERNAL_ERROR} renvoye au frontend sans aucune trace cote serveur est
 * impossible a diagnostiquer : c'etait le comportement observe sur
 * {@code POST /api/v1/planning/agenda/slots}.
 *
 * <p>Ces tests verrouillent la traduction HTTP des pannes d'infrastructure : elles doivent
 * donner un statut actionnable (409 rejouable, 503 dependance indisponible, 500 explicite)
 * plutot qu'un 500 muet.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = request("POST", "/api/v1/planning/agenda/slots");

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    /**
     * Le constructeur de {@link ExceptionHandlerMethodResolver} leve
     * {@code IllegalStateException("Ambiguous @ExceptionHandler method mapped for ...")} au
     * demarrage de l'application si deux handlers revendiquent le meme type : ce test
     * garantit donc a la fois l'absence de mapping ambigu et le choix du handler le plus
     * specifique (comparateur de profondeur d'heritage).
     */
    @Test
    void resolvesTheMostSpecificHandlerWithoutAmbiguity() {
        ExceptionHandlerMethodResolver[] created = new ExceptionHandlerMethodResolver[1];
        assertThatCode(() -> created[0] = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class))
                .doesNotThrowAnyException();
        ExceptionHandlerMethodResolver resolver = created[0];

        assertThat(resolver.hasExceptionMappings()).isTrue();
        assertThat(handlerFor(resolver, OptimisticLockingFailureException.class)).isEqualTo("handleTransientPersistence");
        assertThat(handlerFor(resolver, PessimisticLockingFailureException.class)).isEqualTo("handleTransientPersistence");
        assertThat(handlerFor(resolver, QueryTimeoutException.class)).isEqualTo("handleTransientPersistence");
        assertThat(handlerFor(resolver, CannotGetJdbcConnectionException.class)).isEqualTo("handleTransientDataAccess");
        assertThat(handlerFor(resolver, TransientDataAccessResourceException.class)).isEqualTo("handleTransientDataAccess");
        assertThat(handlerFor(resolver, DataIntegrityViolationException.class)).isEqualTo("handleDataIntegrity");
        assertThat(handlerFor(resolver, IllegalStateException.class)).isEqualTo("handleIllegalState");
        assertThat(handlerFor(resolver, PlanningException.class)).isEqualTo("handlePlanning");
        assertThat(handlerFor(resolver, IllegalArgumentException.class)).isEqualTo("handleBadRequest");
        assertThat(handlerFor(resolver, NullPointerException.class)).isEqualTo("handleUnexpected");
    }

    private static String handlerFor(ExceptionHandlerMethodResolver resolver, Class<? extends Throwable> failure) {
        Method method = resolver.resolveMethodByExceptionType(failure);
        assertThat(method).as("handler resolu pour %s", failure.getSimpleName()).isNotNull();
        return method.getName();
    }

    @Test
    void domainRefusalsKeepTheirBusinessStatus() {
        assertThat(handler.handlePlanning(new PlanningException(PlanningException.Code.CONFLICT,
                "Chevauchement ou trajet insuffisant"), request).getStatusCode().value()).isEqualTo(409);
        assertThat(handler.handlePlanning(new PlanningException(PlanningException.Code.INVALID,
                "Lieu d'exercice inconnu"), request).getStatusCode().value()).isEqualTo(400);
        assertThat(handler.handlePlanning(new PlanningException(PlanningException.Code.FORBIDDEN,
                "Ce lieu n'appartient pas au praticien"), request).getStatusCode().value()).isEqualTo(403);
        assertThat(handler.handlePlanning(new PlanningException(PlanningException.Code.UNAVAILABLE,
                "Agenda indisponible"), request).getStatusCode().value()).isEqualTo(503);
        assertThat(handler.handlePlanning(new PlanningException(PlanningException.Code.NOT_FOUND,
                "Agenda introuvable"), request).getBody().code()).isEqualTo("PLANNING_NOT_FOUND");
    }

    @Test
    void lockContentionIsRetryableConflictNotInternalError() {
        var response = handler.handleTransientPersistence(
                new PessimisticLockingFailureException("Timeout trying to lock table"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("CONCURRENT_UPDATE");
        assertThat(response.getBody().message()).contains("rejouer");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/planning/agenda/slots");
    }

    @Test
    void unreachableDatabaseIsServiceUnavailable() {
        assertThat(handler.handleTransientDataAccess(
                        new CannotGetJdbcConnectionException("Unable to obtain JDBC Connection"), request)
                .getBody().code()).isEqualTo("DATABASE_UNAVAILABLE");

        var response = handler.handleTransientDataAccess(
                new TransientDataAccessResourceException("ressource transitoirement indisponible"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().code()).isEqualTo("DATABASE_UNAVAILABLE");
    }

    @Test
    void integrityFailureIsConflict() {
        var response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("Duplicate entry '1' for key 'PRIMARY'"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("DATA_CONFLICT");
    }

    @Test
    void corruptedAggregateIsReportedWithItsContextInsteadOfSilentInternalError() {
        var response = handler.handleIllegalState(
                new IllegalStateException("Lecture de l'agenda 42 impossible"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().code()).isEqualTo("APPLICATION_STATE_ERROR");
        assertThat(response.getBody().message()).contains("42");
    }

    @Test
    void invalidRequestStaysBadRequest() {
        var response = handler.handleBadRequest(new IllegalArgumentException("Identite agenda immutable"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void unexpectedFailureStillReturnsGenericBody() {
        var response = handler.handleUnexpected(new NullPointerException(), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Une erreur interne est survenue");
    }
}
