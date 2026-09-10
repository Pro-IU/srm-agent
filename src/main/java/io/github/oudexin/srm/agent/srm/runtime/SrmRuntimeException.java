package io.github.oudexin.srm.agent.srm.runtime;

/** Stable API-safe error; internal exception details and credentials are intentionally omitted. */
public class SrmRuntimeException extends IllegalArgumentException {
    private final String code;
    public SrmRuntimeException(String code, String message) { super(message); this.code = code; }
    public String getCode() { return code; }
}
