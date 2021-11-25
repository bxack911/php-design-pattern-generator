package ore.plugins.idea.design.patterns.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import ore.plugins.idea.design.patterns.service.BuilderPatternGenerator;
import ore.plugins.idea.lib.action.OrePluginAction;
import ore.plugins.idea.lib.dialog.SelectStuffDialog;
import ore.plugins.idea.lib.exception.InvalidFileException;
import ore.plugins.idea.lib.model.ui.NameListCelRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BuilderAction extends OrePluginAction {

    private static final String BUILDER_DIALOG_TITLE = "Builder";
    private static final String BUILDER_FIELDS_DIALOG_MESSAGE = "Fields to include:";
    private static final String BUILDER_MANDATORY_FIELDS_DIALOG_MESSAGE = "Mandatory fields:";


    @Override
    public void safeActionPerformed(AnActionEvent anActionEvent) {
        PhpClass psiClass = extractPsiClass(anActionEvent);
        List<Field> candidateFields = psiClass.getFields().stream()
                .filter(this::makeSureIsNotStatic)
                .collect(Collectors.toList());

        SelectStuffDialog<Field> includedFieldsDialog = new SelectStuffDialog<>(
                psiClass.getProject(),
                BUILDER_DIALOG_TITLE, BUILDER_FIELDS_DIALOG_MESSAGE,
                candidateFields, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, new NameListCelRenderer());
        includedFieldsDialog.waitForInput();


        SelectStuffDialog<Field> mandatoryFieldsDialog = new SelectStuffDialog<>(
                psiClass.getProject(),
                BUILDER_DIALOG_TITLE, BUILDER_MANDATORY_FIELDS_DIALOG_MESSAGE,
                includedFieldsDialog.getSelectedStuff(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, new NameListCelRenderer());
        mandatoryFieldsDialog.waitForInput();
        generateCode(psiClass, includedFieldsDialog.getSelectedStuff(), mandatoryFieldsDialog.getSelectedStuff());
    }


    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        safeExecute(() -> {
            super.update(anActionEvent);
            PhpClass psiClass = extractPsiClass(anActionEvent);
            psiClass.getModifier();
            if (psiClass.getModifier().isStatic()) {
                throw new InvalidFileException();

            }
        }, anActionEvent);
    }

    private void generateCode(PhpClass psiClass, List<Field> includedFields, List<Field> mandatoryFields) {
        WriteCommandAction.runWriteCommandAction(psiClass.getProject(), () -> new BuilderPatternGenerator(psiClass, includedFields, mandatoryFields).generateJavaClass());
    }

    private boolean makeSureIsNotStatic(Field psiField) {
        return !psiField.getModifier().isStatic() && !psiField.getModifier().isFinal();
    }

}