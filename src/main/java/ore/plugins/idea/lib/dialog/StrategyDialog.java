package ore.plugins.idea.lib.dialog;

import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiClass;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import ore.plugins.idea.lib.dialog.base.OrePluginDialog;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class StrategyDialog extends OrePluginDialog {

    private static final String STRATEGY_NAME_DIALOG_TEXT = "Give a name for the interface:\n";
    private static final String STRATEGY_REPLICAS_COUNT_DIALOG_TEXT = "Give a count of current class replicas:\n";

    private final JPanel component;
    private JBTextField jbNameField;
    private JBTextField jbReplicasField;

    public StrategyDialog(PhpClass psiClass, String title, String defaultText) {
        super(psiClass.getProject());


        setTitle(title);
        JPanel jPanel = new JPanel();
        jPanel.add(new JBLabel(STRATEGY_NAME_DIALOG_TEXT));
        jbNameField = new JBTextField();
        jbNameField.setEditable(true);
        jbNameField.setFocusable(true);
        jbNameField.setColumns(40);
        jbNameField.setText(defaultText);
        jPanel.add(jbNameField);

        jPanel.add(new JBLabel(STRATEGY_REPLICAS_COUNT_DIALOG_TEXT));
        jbReplicasField = new JBTextField();
        jbReplicasField.setEditable(true);
        jbReplicasField.setFocusable(true);
        jbReplicasField.setColumns(40);
        jbReplicasField.setText("0");
        jPanel.add(jbReplicasField);

        component = jPanel;
        showDialog();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return component;
    }

    public String getName() {
        return jbNameField.getText().trim();
    }

    public String getReplicas() {
        return jbReplicasField.getText().trim();
    }
}