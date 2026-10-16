package com.prdv.rdv.planning.domain;

import com.prdv.rdv.common.domain.DomainException;

public class PlanningException extends DomainException {
    public enum Code { NOT_FOUND, FORBIDDEN, CONFLICT, INVALID, UNAVAILABLE }
    private final Code code;
    public PlanningException(Code code, String message) { super(message); this.code = code; }
    public Code code() { return code; }
    public static void require(boolean condition, Code code, String message) {
        if (!condition) throw new PlanningException(code, message);
    }
}
