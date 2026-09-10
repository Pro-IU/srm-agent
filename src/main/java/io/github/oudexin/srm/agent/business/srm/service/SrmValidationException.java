package io.github.oudexin.srm.agent.business.srm.service;

/** A deterministic, client-safe validation or controlled-action rejection. */
public class SrmValidationException extends RuntimeException {
    public SrmValidationException(String message) {
        super(message);
    }
}
