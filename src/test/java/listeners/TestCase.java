package listeners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Plain-English description of a test case, shown in the report as
 * Test Case ID | Type | Test Steps | Expected Result | Actual Result.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestCase {

    /**
     * Fixed test case ID such as "ALD007". When set, the report uses it (and sorts by it), so
     * IDs stay the same even when tests run in parallel. When empty, IDs follow run order.
     */
    String id() default "";

    /** Positive, Negative or Edge. */
    Type type();

    /** What the test does, written for someone who has never seen the code. */
    String steps();

    /** What should happen if the app works correctly. */
    String expected();

    enum Type {
        POSITIVE("Positive"), NEGATIVE("Negative"), EDGE("Edge");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
