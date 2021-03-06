package org.arquillian.smart.testing.vcs.git;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;
import org.arquillian.smart.testing.TestSelection;
import org.arquillian.smart.testing.api.TestVerifier;
import org.arquillian.smart.testing.configuration.Configuration;
import org.arquillian.smart.testing.hub.storage.ChangeStorage;
import org.arquillian.smart.testing.logger.Log;
import org.arquillian.smart.testing.logger.Logger;
import org.arquillian.smart.testing.scm.Change;
import org.arquillian.smart.testing.scm.spi.ChangeResolver;
import org.arquillian.smart.testing.spi.JavaSPILoader;
import org.arquillian.smart.testing.spi.TestExecutionPlanner;

import static org.arquillian.smart.testing.scm.ChangeType.ADD;

public class NewTestsDetector implements TestExecutionPlanner {

    private static final Logger logger = Log.getLogger();

    private final ChangeResolver changeResolver;
    private final ChangeStorage changeStorage;
    private final File projectDir;
    private final TestVerifier testVerifier;
    private final Configuration configuration;

    // Temporary before introducing proper DI
    public NewTestsDetector(File projectDir, TestVerifier testVerifier, Configuration configuration) {
        this(new JavaSPILoader().onlyOne(ChangeResolver.class).get(),
            new JavaSPILoader().onlyOne(ChangeStorage.class).get(),
            projectDir,
            testVerifier,
            configuration);
    }

    public NewTestsDetector(ChangeResolver changeResolver, ChangeStorage changeStorage, File projectDir,
        TestVerifier testVerifier, Configuration configuration) {
        this.changeResolver = changeResolver;
        this.changeStorage = changeStorage;
        this.projectDir = projectDir;
        this.testVerifier = testVerifier;
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return "new";
    }

    @Override
    public final Collection<TestSelection> getTests() {
        final Collection<Change> changes = changeStorage.read(projectDir)
            .orElseGet(() -> {
                logger.warn("No cached changes detected... using direct resolution");
                return changeResolver.diff(projectDir, configuration);
        });

        return changes.stream()
            .filter(change -> ADD.equals(change.getChangeType()))
            .filter(change -> testVerifier.isTest(change.getLocation()))
            .map(change -> new TestSelection(change.getLocation(), getName()))
            .collect(Collectors.toList());
    }

}
