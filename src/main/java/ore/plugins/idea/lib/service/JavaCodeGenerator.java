package ore.plugins.idea.lib.service;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import ore.plugins.idea.lib.provider.ConstructorProvider;
import ore.plugins.idea.lib.service.base.OrePluginGenerator;

public abstract class JavaCodeGenerator extends OrePluginGenerator implements ConstructorProvider {

    protected static final String DEFAULT_JAVA_SRC_PATH = "/src/main/java/";

    public JavaCodeGenerator(PhpClass psiClass) {
        super(psiClass);
    }

    public abstract void generateJavaClass();
}
