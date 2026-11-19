package com.raviranjan.os.ai;

/** Single source of truth for how the assistant names itself and for the "not in the portfolio" reply. */
public final class AssistantIdentity {

    /** The only name the assistant uses for itself (en dash written as an escape so file encoding never matters). */
    public static final String NAME = "Ask Raviranjan \u2013 AI Portfolio Assistant";

    /** Returned verbatim whenever the portfolio does not contain the requested information. */
    public static final String UNKNOWN_ANSWER = "I don't have that information in Raviranjan's portfolio.";

    private AssistantIdentity() { }
}
