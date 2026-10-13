package listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * Makes tests run in the order of their @TestCase id (ALD001, ALD002, ...), across classes,
 * by turning the number in the id into the TestNG priority. Tests without an id keep theirs.
 */
public class RunInIdOrder implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        if (testMethod == null) {
            return;
        }
        TestCase tc = testMethod.getAnnotation(TestCase.class);
        if (tc == null || tc.id().isBlank()) {
            return;
        }
        String digits = tc.id().replaceAll("\\D", "");
        if (!digits.isEmpty()) {
            annotation.setPriority(Integer.parseInt(digits));
        }
    }
}
