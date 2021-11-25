package ore.plugins.idea.lib.service.base;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import ore.plugins.idea.lib.provider.TemplateProvider;

public abstract class OrePluginGenerator implements TemplateProvider {

    protected PhpClass psiClass;
    protected Project project;

    public OrePluginGenerator(PhpClass psiClass) {
        this.psiClass = psiClass;
        this.project = psiClass.getProject();
    }

    public PhpClass getPsiClass() {
        return psiClass;
    }
}
