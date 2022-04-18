package ore.plugins.idea.design.patterns.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.impl.*;
import ore.plugins.idea.design.patterns.service.FactoryPatternGenerator;
import ore.plugins.idea.design.patterns.utils.ClassNameValidator;
import ore.plugins.idea.design.patterns.utils.PhpPsiClassGeneratorUtils;
import ore.plugins.idea.lib.action.OrePluginAction;
import ore.plugins.idea.lib.dialog.InputDialog;
import ore.plugins.idea.lib.dialog.SelectStuffDialog;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FactoryAction extends OrePluginAction implements ClassNameValidator {
    private static final String FACTORY_DIALOG_TITLE = "Factory";
    private static final String FACTORY_NAME_DIALOG_TEXT = "Give a name for the Factory and the Enum class (\"Factory\" and \"Enum\" suffixes will be automatically added):";
    private static final String FACTORY_IMPLEMENTORS_CHOICE_DIALOG_TEXT = "Implementors to include:";


    private static final String FACTORY_SUFFIX = "Factory";

    private void generateCode(PhpClass psiClass, List<String> classesToBuild, List<String> useStatements, String factoryName) {
        WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> new FactoryPatternGenerator(psiClass, classesToBuild, useStatements, factoryName).generateJavaClass());
    }

    @Override
    public void safeActionPerformed(AnActionEvent anActionEvent) {
        PhpClass psiClass = extractPsiClass(anActionEvent);

        List<String> classes = psiClass.getMethods().stream().map(method -> {
            for (PsiElement elem : method.getLastChild().getChildren()) {
                if (elem.getClass().toString().equals(PhpReturnImpl.class.toString())) {
                    for (PsiElement returnElem : elem.getChildren()) {
                        if (returnElem.getClass().toString().equals(NewExpressionImpl.class.toString())) {
                            return returnElem.getText();
                            /*for (int i = 0; i < 40; i++) {
                                PsiElement element = returnElem.findElementAt(i);
                                ClassReference classRefInReturn = PsiTreeUtil.getParentOfType(element, ClassReference.class);

                                if (classRefInReturn != null) {
                                    String projectPath = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, Objects.requireNonNull(psiClass.getProject().getBasePath()));
                                    VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(projectPath + "/" + classRefInReturn.getName());
                                    return virtualFile.getName();
                                    PsiFile psiFile = PsiManager.getInstance(psiClass.getProject()).findFile(virtualFile);
                                    PsiFile classFile = psiFile.getContainingDirectory().findFile(FileUtil.getNameWithoutExtension(psiFile.getName()) + ".php");

                                    for (int j = 0; j < 40; j++) {
                                        PsiElement elementInFile = classFile.findElementAt(i);
                                        PhpClass classInReturn = PsiTreeUtil.getParentOfType(elementInFile, PhpClass.class);

                                        if (classInReturn != null) {
                                            return classInReturn.getName();
                                        }
                                    }
                                }
                            }*/
                        }
                    }
                }
            }

            return null;
        }).collect(Collectors.toList());

        InputDialog selector = new InputDialog(psiClass, FACTORY_DIALOG_TITLE, FACTORY_NAME_DIALOG_TEXT, psiClass.getName() + "Factory");
        selector.waitForInput();
        String selectedName = selector.getInput().replace(FACTORY_SUFFIX, "");
        String factoryName = validateClassNameOrThrow(psiClass, selectedName.concat(FACTORY_SUFFIX));

        SelectStuffDialog<String> selectorClass = new SelectStuffDialog<>(
                psiClass.getProject(),
                FACTORY_DIALOG_TITLE, FACTORY_IMPLEMENTORS_CHOICE_DIALOG_TEXT,
                classes, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selector.waitForInput();

        List<String> selectedClasses = selectorClass.getSelectedStuff();
        List<String> useStatements = (new PhpPsiClassGeneratorUtils(psiClass, "", psiClass.getName())).getUseListInFile(psiClass.getContainingFile(), getClassNamesByReturnExpression(classes));

        generateCode(psiClass, selectedClasses, useStatements, factoryName);
    }

    private List<String> getClassNamesByReturnExpression(List<String> expressions) {
        return expressions.stream()
                .map(expression -> {
                    if (expression == null || expression == "") {
                        return "";
                    }
                    String[] spacesSplitExpr = expression.split("new ");
                    String[] openBracketSplitExpr = spacesSplitExpr[1].split("\\(");
                    return openBracketSplitExpr[0];
                })
                .collect(Collectors.toList());
    }
}
