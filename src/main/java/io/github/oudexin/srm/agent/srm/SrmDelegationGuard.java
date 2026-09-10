package io.github.oudexin.srm.agent.srm;

/** Prevents accidental loops even if future roles add delegation. */
public final class SrmDelegationGuard {
    public static final int MAX_DELEGATION_DEPTH = 2;
    public static final int MAX_DELEGATION_COUNT = 3;
    private int count;

    public boolean tryEnter(int depth) {
        if (depth > MAX_DELEGATION_DEPTH || count >= MAX_DELEGATION_COUNT) return false;
        count++;
        return true;
    }

    public int count() { return count; }
}
