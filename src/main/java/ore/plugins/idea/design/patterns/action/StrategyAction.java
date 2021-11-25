package ore.plugins.idea.design.patterns.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.Method;
import ore.plugins.idea.design.patterns.service.StrategyPatternGenerator;
import ore.plugins.idea.design.patterns.utils.ClassNameValidator;
import ore.plugins.idea.lib.action.OrePluginAction;
import ore.plugins.idea.lib.dialog.InputDialog;
import ore.plugins.idea.lib.dialog.SelectStuffDialog;
import ore.plugins.idea.lib.dialog.StrategyDialog;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

public class StrategyAction extends OrePluginAction implements ClassNameValidator {

    private static final String STRATEGY_DIALOG_TITLE = "Strategy";
    private static final String STRATEGY_METHODS_DIALOG_TEXT = "Methods to include:";
    private static Integer ReplicasCount = 0;


    @Override
    public void safeActionPerformed(AnActionEvent anActionEvent) {
        PhpClass psiClass = extractPsiClass(anActionEvent);
        StrategyDialog strategyDialog = new StrategyDialog(psiClass, STRATEGY_DIALOG_TITLE, psiClass.getName() + "Strategy");
        strategyDialog.waitForInput();
        String strategyName = validateClassNameOrThrow(psiClass, strategyDialog.getName());

        String strategyReplicasCount = strategyDialog.getReplicas();
        try {
            ReplicasCount = Integer.parseInt(strategyReplicasCount);
        } catch (NumberFormatException | NullPointerException e) {
            ReplicasCount = 0;
        }

        List<Method> allExceptConstructor = psiClass.getMethods().stream().filter(method -> method.getMethodType(true) != Method.MethodType.CONSTRUCTOR).collect(Collectors.toList());
        SelectStuffDialog<Method> strategyMethodsDialog = new SelectStuffDialog<>(
                psiClass.getProject(),
                STRATEGY_DIALOG_TITLE, STRATEGY_METHODS_DIALOG_TEXT,
                allExceptConstructor, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        strategyMethodsDialog.waitForInput();
        generateCode(psiClass, strategyName, strategyMethodsDialog.getSelectedStuff());
    }

    private void generateCode(PhpClass psiClass, String strategyName, List<Method> selectedMethods) {
        WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> new StrategyPatternGenerator(psiClass, strategyName, selectedMethods, ReplicasCount).generateJavaClass());
    }
}
