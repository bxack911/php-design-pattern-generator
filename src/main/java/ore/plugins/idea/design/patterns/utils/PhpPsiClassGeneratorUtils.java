package ore.plugins.idea.design.patterns.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpUse;
import com.jetbrains.php.lang.psi.elements.impl.*;

import java.util.ArrayList;
import java.util.List;

public class PhpPsiClassGeneratorUtils {
    PhpClass psiClass;
    String namespace;
    String className;

    public PhpPsiClassGeneratorUtils (PhpClass psiClass, String namespace, String className) {
        this.psiClass = psiClass;
        this.namespace = namespace;
        this.className = className;
    }

    public PhpClass generateClass(List<String> useStatements) {
        PhpClass phpClass = PhpPsiElementFactory.createPhpPsiFromText(this.psiClass.getProject(), PhpClass.class, "class " + this.className + " { }");
        PsiFile classFile = this.psiClass.getContainingFile().getContainingDirectory().createFile(this.className.concat(".php"));
        classFile.addAfter(PhpPsiElementFactory.createPhpPsiFromText(this.psiClass.getProject(), PhpPsiElement.class, "\nnamespace " + this.namespace + ";\n\n"), classFile.getFirstChild());
        classFile.addAfter(PhpPsiElementFactory.createWhiteSpace(this.psiClass.getProject()), classFile.getLastChild());

        for (String useStatement : useStatements) {
            classFile.addAfter(PhpPsiElementFactory.createUseStatement(this.psiClass.getProject(), useStatement, ""), classFile.getLastChild());
        }

        classFile.addAfter(PhpPsiElementFactory.createWhiteSpace(this.psiClass.getProject()), classFile.getLastChild());
        classFile.addAfter(phpClass, classFile.getLastChild());
        classFile.addAfter(PhpPsiElementFactory.createWhiteSpace(this.psiClass.getProject()), classFile.getLastChild());

        PhpClass newClass = this.getClassInFile(classFile);

        return (newClass != null ? newClass : phpClass);
    }

    public PhpClass getClassInFile(PsiFile classFile) {
        for (int i = 0; i < 40; i++) {
            PsiElement element = classFile.findElementAt(i);
            PhpClass classInReturn = PsiTreeUtil.getParentOfType(element, PhpClass.class);

            if (classInReturn != null) {
                return classInReturn;
            }
        }

        return null;
    }

    public List<String> getUseListInFile(PsiFile psiFile, List<String> includeClasses) {
        List<String> useStatements = new ArrayList<>();

        for (PsiElement elem : psiFile.getFirstChild().getChildren()) {
            if (elem.getClass().toString().equals(PhpNamespaceImpl.class.toString())) {
                for (PsiElement namespaceElem : elem.getChildren()) {
                    if (namespaceElem.getClass().toString().equals(GroupStatementSimpleImpl.class.toString())) {
                        for (PsiElement useListElem : namespaceElem.getChildren()) {
                            if (useListElem.getClass().toString().equals(PhpUseListImpl.class.toString())) {
                                for (PsiElement useElem : useListElem.getChildren()) {
                                    if (useElem.getClass().toString().equals(PhpUseImpl.class.toString())) {
                                        String[] classParts = useElem.getText().split("\\\\");
                                        String className = classParts[classParts.length - 1];

                                        if (includeClasses.contains(className)) {
                                            useStatements.add("\\" + useElem.getText());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return useStatements;
    }
}
