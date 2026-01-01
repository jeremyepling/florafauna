package net.j40climb.florafauna.test;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom TestReporter that outputs colored test results to the console.
 * Uses ANSI escape codes for coloring (works in most terminals).
 */
public class ColoredTestReporter implements TestReporter {

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    // Bright/bold variants
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";

    private final List<GameTestInfo> passedTests = new ArrayList<>();
    private final List<GameTestInfo> failedRequiredTests = new ArrayList<>();
    private final List<GameTestInfo> failedOptionalTests = new ArrayList<>();

    @Override
    public void onTestSuccess(GameTestInfo testInfo) {
        passedTests.add(testInfo);
        String testName = testInfo.id().toString();
        System.out.println(GREEN + BOLD + "  PASS " + RESET + GREEN + testName + RESET);
    }

    @Override
    public void onTestFailed(GameTestInfo testInfo) {
        String testName = testInfo.id().toString();
        String errorMsg = Util.describeError(testInfo.getError());

        if (testInfo.isRequired()) {
            failedRequiredTests.add(testInfo);
            System.out.println(RED + BOLD + "  FAIL " + RESET + RED + testName + RESET);
            System.out.println(RED + "       " + errorMsg + RESET);
        } else {
            failedOptionalTests.add(testInfo);
            System.out.println(YELLOW + BOLD + "  WARN " + RESET + YELLOW + testName + " (optional)" + RESET);
            System.out.println(YELLOW + "       " + errorMsg + RESET);
        }
    }

    @Override
    public void finish() {
        int total = passedTests.size() + failedRequiredTests.size() + failedOptionalTests.size();
        int passed = passedTests.size();
        int failedRequired = failedRequiredTests.size();
        int failedOptional = failedOptionalTests.size();

        System.out.println();
        System.out.println(CYAN + BOLD + "════════════════════════════════════════════════════════════" + RESET);
        System.out.println(CYAN + BOLD + "                      TEST SUMMARY                           " + RESET);
        System.out.println(CYAN + BOLD + "════════════════════════════════════════════════════════════" + RESET);
        System.out.println();

        // Show passed count
        if (passed > 0) {
            System.out.println(BRIGHT_GREEN + BOLD + "  " + passed + " test(s) passed" + RESET);
        }

        // Show failed required count
        if (failedRequired > 0) {
            System.out.println(BRIGHT_RED + BOLD + "  " + failedRequired + " required test(s) FAILED" + RESET);
            for (GameTestInfo test : failedRequiredTests) {
                System.out.println(RED + "    - " + test.id() + ": " + Util.describeError(test.getError()) + RESET);
            }
        }

        // Show failed optional count
        if (failedOptional > 0) {
            System.out.println(BRIGHT_YELLOW + "  " + failedOptional + " optional test(s) failed" + RESET);
            for (GameTestInfo test : failedOptionalTests) {
                System.out.println(YELLOW + "    - " + test.id() + RESET);
            }
        }

        System.out.println();

        // Final verdict
        if (failedRequired == 0) {
            System.out.println(BRIGHT_GREEN + BOLD + "  ✓ ALL TESTS PASSED (" + passed + "/" + total + ")" + RESET);
        } else {
            System.out.println(BRIGHT_RED + BOLD + "  ✗ TESTS FAILED (" + failedRequired + " required failures)" + RESET);
        }

        System.out.println();
        System.out.println(CYAN + BOLD + "════════════════════════════════════════════════════════════" + RESET);
        System.out.println();
    }
}
