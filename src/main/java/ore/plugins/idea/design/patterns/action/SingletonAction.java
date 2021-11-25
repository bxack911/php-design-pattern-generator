package ore.plugins.idea.design.patterns.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import ore.plugins.idea.design.patterns.service.SingletonPatternGenerator;
import ore.plugins.idea.lib.action.OrePluginAction;


public class SingletonAction extends OrePluginAction {

    @Override
    public void safeActionPerformed(AnActionEvent anActionEvent) {
        PhpClass psiClass = extractPsiClass(anActionEvent);
        generateCode(psiClass);
    }

    private void generateCode(PhpClass psiClass) {
        WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> new SingletonPatternGenerator(psiClass).generateJavaClass());
    }
}
