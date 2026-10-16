package com.prdv.rdv.common.web;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduction transversale des exceptions du domaine en reponses HTTP.
 * Les adapteurs REST ne connaissent pas les regles metier : c'est ici que
 * le code d'erreur fonctionnel devient un statut HTTP.
 *
 * <p>Deux regles s'appliquent a tous les handlers :
 * <ol>
 *     <li>une erreur serveur (5xx) est <b>toujours</b> journalisee avec sa stack trace :
 *         un 500 renvoye au client sans trace cote serveur est impossible a diagnostiquer ;</li>
 *     <li>une panne de dependance (verrou perdu, base injoignable) n'est pas un 500 :
 *         elle recoit un statut actionnable (409 rejouable, 503 indisponible).</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trace la requete en echec cote serveur : sans cet appel, un 500 renvoye au client
     * (par exemple {@code POST /api/v1/planning/agenda/slots}) ne laissait AUCUNE trace
     * dans les logs et la cause reelle restait invisible.
     */
    private static void trace(HttpServletRequest request, Exception ex) {
        log.error("{} {} -> {} : {}", request.getMethod(), request.getRequestURI(),
                ex.getClass().getName(), ex.getMessage(), ex);
    }

    @ExceptionHandler(com.prdv.rdv.planning.domain.PlanningException.class)
    public ResponseEntity<ApiError> handlePlanning(com.prdv.rdv.planning.domain.PlanningException ex,
                                                   HttpServletRequest request) {
        int status = switch (ex.code()) {
            case NOT_FOUND -> 404;
            case FORBIDDEN -> 403;
            case CONFLICT -> 409;
            case INVALID -> 400;
            case UNAVAILABLE -> 503;
        };
        if (status >= 500) {
            trace(request, ex);
        } else {
            log.debug("{} {} -> refus metier {} : {}", request.getMethod(), request.getRequestURI(),
                    ex.code(), ex.getMessage());
        }
        return ResponseEntity.status(status).body(ApiError.of(status, "PLANNING_" + ex.code().name(),
                ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IamException.class)
    public ResponseEntity<ApiError> handleIam(IamException ex, HttpServletRequest request) {
        IamErrorCode code = ex.getErrorCode();
        log.debug("{} {} -> IAM {} : {}", request.getMethod(), request.getRequestURI(), code, ex.getMessage());
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiError.of(code.getHttpStatus(), code.name(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ProfileException.class)
    public ResponseEntity<ApiError> handleProfile(ProfileException ex, HttpServletRequest request) {
        ProfileErrorCode code = ex.getErrorCode();
        log.debug("{} {} -> PROFILE {} : {}", request.getMethod(), request.getRequestURI(), code, ex.getMessage());
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiError.of(code.getHttpStatus(), code.name(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        log.debug("{} {} -> parametres invalides : {}", request.getMethod(), request.getRequestURI(), errors);
        return ResponseEntity.badRequest()
                .body(ApiError.validation(HttpStatus.BAD_REQUEST.value(), "Parametres invalides", errors,
                        request.getRequestURI()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MultipartException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        // Corps JSON irrecevable, parametre manquant ou argument illegal : la cause exacte
        // (champ en erreur, valeur attendue) doit rester consultable cote serveur.
        log.debug("{} {} -> requete invalide ({}) : {}", request.getMethod(), request.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "VALIDATION_ERROR",
                        "Requete invalide : " + ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex,
                                                     HttpServletRequest request) {
        log.debug("{} {} -> contrainte de validation : {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "VALIDATION_ERROR", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                         HttpServletRequest request) {
        log.debug("{} {} -> authentification refusee ({})", request.getMethod(), request.getRequestURI(),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "UNAUTHORIZED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        log.debug("{} {} -> acces refuse ({})", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "FORBIDDEN", "Acces refuse : permission manquante", request.getRequestURI()));
    }

    /**
     * Echec d'integrite ou de contrainte SQL (cle dupliquee, colonne {@code nullable=false}
     * alimentee a {@code NULL}) : la requete viole une regle de persistance, ce qui est un
     * conflit de donnees (409) et non une erreur interne.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        trace(request, ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "DATA_CONFLICT",
                        "Operation refusee : donnees en conflit avec une contrainte existante",
                        request.getRequestURI()));
    }

    /**
     * Concours d'acces : verrou pessimiste perdu ou attente trop longue (deadlock, timeout)
     * et version optimistic locking depassee. Le planning serialise ses ecritures derriere un
     * verrou global ({@code PlanningCoordinator}) : un lot de requetes frontend paralleles peut
     * donc depasser le delai d'attente de la base. C'est rejouable, pas un 500.
     *
     * <p>{@code TransientDataAccessException} n'est volontairement PAS liste ici : il est
     * traite par {@link #handleTransientDataAccess} (503). Spring retient le handler dont le
     * type declare est le plus proche de l'exception, donc {@code ConcurrencyFailureException}
     * et {@code QueryTimeoutException} (toutes deux plus specifiques) tombent bien ici.
     */
    @ExceptionHandler({ConcurrencyFailureException.class, QueryTimeoutException.class})
    public ResponseEntity<ApiError> handleTransientPersistence(Exception ex, HttpServletRequest request) {
        trace(request, ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "CONCURRENT_UPDATE",
                        "L'agenda est verrouille par une autre operation : merci de rejouer la requete",
                        request.getRequestURI()));
    }

    /**
     * Dependance de persistance indisponible : base injoignable, connexion JDBC impossible
     * ({@code CannotGetJdbcConnectionException} herite de
     * {@code NonTransientDataAccessResourceException}), ressource transitoirement
     * inaccessible. Le statut correct est 503 pour que le client sache qu'un nouvel essai a
     * du sens. Les autres echecs SQL non transitoires (requete invalide, usage incorrect de
     * l'API de donnees) restent traites par le handler generique en 500, desormais journalise.
     */
    @ExceptionHandler({TransientDataAccessException.class, NonTransientDataAccessResourceException.class})
    public ResponseEntity<ApiError> handleTransientDataAccess(DataAccessException ex,
                                                              HttpServletRequest request) {
        trace(request, ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of(503, "DATABASE_UNAVAILABLE",
                        "Persistance indisponible : merci de reessayer", request.getRequestURI()));
    }

    /**
     * Invariant d'application casse : agregat illisible en base, verrou de planning absent,
     * verrouillage demande hors transaction ({@code TransactionRequiredException} herite
     * d'{@code IllegalStateException}). Le message est remonte au client et la cause complete
     * est journalisee.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        trace(request, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "APPLICATION_STATE_ERROR",
                        "Etat interne invalide : " + ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        trace(request, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "INTERNAL_ERROR", "Une erreur interne est survenue", request.getRequestURI()));
    }
}
