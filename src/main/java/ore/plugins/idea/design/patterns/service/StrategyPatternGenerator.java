package ore.plugins.idea.design.patterns.service;

import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import ore.plugins.idea.lib.service.JavaCodeGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StrategyPatternGenerator extends JavaCodeGenerator {

    private String psiPackageStatement;
    private List<Method> psiMethods;
    private String strategyName;
    private String strategyInterfaceName;
    private Integer replicasCount = 0;

    public StrategyPatternGenerator(PhpClass psiClass, String strategyName, List<Method> psiMethods, Integer replicasCount) {
        super(psiClass);
        this.psiMethods = psiMethods;
        this.strategyName = strategyName;
        this.strategyInterfaceName = strategyName + "Interface";
        this.psiPackageStatement = psiClass.getNamespaceName().replace("\\", "");
        this.replicasCount = replicasCount;
    }

    @Override
    public void generateJavaClass() {
        PhpClass interfaceClass = generateInterfaceClass();
        PsiFile interfaceFile = psiClass.getContainingFile().getContainingDirectory().createFile(strategyInterfaceName.concat(".php"));
        interfaceFile.addAfter(PhpPsiElementFactory.createPhpPsiFromText(psiClass.getProject(), PhpPsiElement.class, "\nnamespace " + psiPackageStatement + ";\n\n"), interfaceFile.getFirstChild());
        interfaceFile.addAfter(PhpPsiElementFactory.createWhiteSpace(psiClass.getProject()), interfaceFile.getLastChild());
        interfaceFile.addAfter(interfaceClass, interfaceFile.getLastChild());
        interfaceFile.addAfter(PhpPsiElementFactory.createWhiteSpace(psiClass.getProject()), interfaceFile.getLastChild());

        if (psiClass.getImplementedInterfaces().length > 0) {
            String interfaces = Arrays.stream(psiClass.getImplementedInterfaces())
                    .map(psiInterface -> (!psiInterface.getName().equals(interfaceClass.getName())) ? psiInterface.getName() : null)
                    .collect(Collectors.joining(", "));

            psiClass.getImplementsList().delete();
            psiClass.getImplementsList().add(PhpPsiElementFactory.createImplementsList(psiClass.getProject(), interfaces));
        } else {
            psiClass.getImplementsList().add(PhpPsiElementFactory.createImplementsList(psiClass.getProject(), strategyInterfaceName));
        }


        generateReplicas();
    }

    private PhpClass generateInterfaceClass() {
        PhpClass interfaceClass = PhpPsiElementFactory.createPhpPsiFromText(psiClass.getProject(), PhpClass.class, "interface " + strategyInterfaceName + " { }");
        for (Method psiMethod : psiMethods) {
            StringBuilder methodSb = new StringBuilder();
            methodSb.append("public function " + psiMethod.getName() + "(");
            String parameters = Arrays.stream(psiMethod.getParameters())
                    .map(psiParameter -> psiParameter.getType() + " " + psiParameter.getName())
                    .collect(Collectors.joining(", "));
            methodSb.append(parameters).append(");");
            Method newPsiMethod = PhpPsiElementFactory.createMethod(psiClass.getProject(), methodSb.toString());
            interfaceClass.addBefore(newPsiMethod, interfaceClass.getLastChild());
        }
        return interfaceClass;
    }

    private void generateReplicas() {
        String originClassName = psiClass.getName();
        for (int i = 0; i < this.replicasCount; i++) {
            int classNumber = i + 1;
            String className = psiClass.getName() + classNumber;
            PhpClass copiedClass = psiClass;
            copiedClass.setName(className);

            psiClass.getContainingFile().getContainingDirectory().copyFileFrom(className.concat(".php"), psiClass.getContainingFile());
            psiClass.setName(originClassName);
        }
    }
}
