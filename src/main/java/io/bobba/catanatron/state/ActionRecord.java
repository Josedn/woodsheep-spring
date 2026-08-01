package io.bobba.catanatron.state;

/**
 * Pairs an Action with the result of applying it. Used for game logs, full replay, and undo.
 *
 * <p>Result shape by action type: ROLL — int[2] (dice values) DISCARD — Resource[] discarded
 * MOVE_ROBBER — Resource stolen, or null BUY_DEVELOPMENT_CARD — DevCard drawn all others — null
 * (deterministic)
 */
public record ActionRecord(Action action, Object result) {}
