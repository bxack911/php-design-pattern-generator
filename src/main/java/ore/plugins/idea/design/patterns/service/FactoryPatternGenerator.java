package ore.plugins.idea.design.patterns.service;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.impl.NewExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpReturnImpl;
import ore.plugins.idea.design.patterns.utils.PhpPsiClassGeneratorUtils;
import ore.plugins.idea.lib.service.JavaCodeGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static ore.plugins.idea.lib.utils.FormatUtils.toFirstLetterUpperCase;

public class FactoryPatternGenerator extends JavaCodeGenerator {
    String factoryName;
    List<String> classesToBuild;
    List<String> useStatements;
    private String psiPackageStatement;
    public FactoryPatternGenerator(PhpClass psiClass, List<String> classesToBuild, List<String> useStatements, String factoryName) {
        super(psiClass);
        this.factoryName = factoryName;
        this.classesToBuild = classesToBuild;
        this.useStatements = useStatements;
        this.psiPackageStatement = psiClass.getNamespaceName().replace("\\", "");
    }

    public void generateJavaClass() {
        PhpClass phpClass = (new PhpPsiClassGeneratorUtils(psiClass, this.psiPackageStatement, this.factoryName)).generateClass(this.useStatements);
        generateCreators(phpClass);
        replaceReturns();
    }

    public void generateCreators(PhpClass factoryClass)
    {
        for (String classToBuild : this.classesToBuild) {
            String[] spacesSplitExpr = classToBuild.split("new ");
            String[] openBracketSplitExpr = spacesSplitExpr[1].split("\\(");
            String className = openBracketSplitExpr[0];

            String[] closedBracketSplitExpr = openBracketSplitExpr[1].split("\\)");
            String arguments = closedBracketSplitExpr[0];

            String upperName = toFirstLetterUpperCase(Objects.requireNonNull(className));


            String content = "public function create" + upperName + "(" + arguments + "): " + className + " {return " + classToBuild + ";}";
            factoryClass.addBefore(PhpPsiElementFactory.createMethod(factoryClass.getProject(), content), factoryClass.getLastChild());
        }
    }

    public void replaceReturns() {
        for (Method method : this.psiClass.getMethods()) {
            for (PsiElement elem : method.getLastChild().getChildren()) {
                if (elem.getClass().toString().equals(PhpReturnImpl.class.toString())) {
                    for (PsiElement returnElem : elem.getChildren()) {
                        if (returnElem.getClass().toString().equals(NewExpressionImpl.class.toString())) {
                            String[] spacesSplitExpr = returnElem.getText().split("new ");
                            String[] openBracketSplitExpr = spacesSplitExpr[1].split("\\(");
                            String className = openBracketSplitExpr[0];

                            String[] closedBracketSplitExpr = openBracketSplitExpr[1].split("\\)");
                            String arguments = closedBracketSplitExpr[0];

                            elem.replace(PhpPsiElementFactory.createStatement(this.psiClass.getProject(), "return (new " + this.factoryName + "())->create" + className + "(" + arguments + ");"));
                        }
                    }
                }
            }
        }
    }
}
