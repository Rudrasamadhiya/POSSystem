package com.rudra.pos;

import java.util.Objects;

/**
 * Tiny zero-dependency assertion + test-tracking helper. Keeping the test
 * harness in plain Java means the project compiles and self-tests with nothing
 * but a JDK — no JUnit download required (which also keeps CI hermetic).
 */
public class Assert {

    private int passed;
    private int failed;

    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public void check(String name, boolean condition) {
        if (condition) {
            pass(name);
        } else {
            fail(name, "expected condition to be true");
        }
    }

    public void assertEquals(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            pass(name);
        } else {
            fail(name, "expected <" + expected + "> but was <" + actual + ">");
        }
    }

    /** Assert that running {@code action} throws an exception of (a subtype of) {@code type}. */
    public void assertThrows(String name, Class<? extends Throwable> type, ThrowingRunnable action) {
        try {
            action.run();
            fail(name, "expected " + type.getSimpleName() + " but nothing was thrown");
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) {
                pass(name);
            } else {
                fail(name, "expected " + type.getSimpleName()
                        + " but got " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage());
            }
        }
    }

    private void pass(String name) {
        passed++;
        System.out.println("  PASS  " + name);
    }

    private void fail(String name, String detail) {
        failed++;
        System.out.println("  FAIL  " + name + "  —  " + detail);
    }

    public int passed() {
        return passed;
    }

    public int failed() {
        return failed;
    }

    public boolean allPassed() {
        return failed == 0;
    }
}
