package software.tnb.common.deployment;

import software.tnb.common.service.ConfigurableService;
import software.tnb.common.utils.JUnitUtils;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Set;

/**
 * Resource that is able to be reused between multiple tests to avoid multiple deploys/undeploys.
 */
public interface ReusableOpenshiftDeployable extends OpenshiftDeployable {
    /**
     * Cleanup the resource itself after running the tests - delete database tables, remove files, etc.
     */
    void cleanup();

    default void beforeAll(ExtensionContext extensionContext) throws Exception {
        // Deploy does "deploy" (if it is not already deployed) + wait until it's ready
        deploy();
        openResources();
    }

    default void afterAll(ExtensionContext extensionContext) throws Exception {
        if (this.isExtensionStillNeeded(extensionContext)) {
            cleanup();
            closeResources();
        } else {
            OpenshiftDeployable.super.afterAll(extensionContext);
        }
    }

    /***
     * Method check if this ReusableOpenshiftDeployable service in needed in the next test cases
     * Upon on the basic check from JUnitUtils, it checks if this class extends ConfigurableService and
     * if yes, it checks also whether the Service with same configuration is needed (if not, it can be undeployed)
     */
    private boolean isExtensionStillNeeded(ExtensionContext extensionContext){
        if (!JUnitUtils.isExtensionStillNeeded(extensionContext, this.getClass())){
            return false;
        } else {
            // This extension is needed in the future tests, but if the extension extends also configurable service class,
            // the configurations need to be checked
            if (ConfigurableService.class.isAssignableFrom(this.getClass())) {
                Set<ClassBasedTestDescriptor> testClasses = JUnitUtils.getAllNextTestClasses(extensionContext);
                boolean found = testClasses.stream().anyMatch(
                    testDescriptor -> AnnotationSupport.findAnnotatedFieldValues(testDescriptor.getTestClass(), RegisterExtension.class)
                        .stream()
                        // filter only this type of extension
                        .filter( registerExtension -> registerExtension.getClass().isAssignableFrom(this.getClass()))
                        // check if configurations are same
                        .anyMatch(registerExtension -> ((ConfigurableService)registerExtension).getConfiguration().equals(((ConfigurableService)this).getConfiguration()))
                );
                return found;
            } else {
                return true;
            }
        }
    }
}
