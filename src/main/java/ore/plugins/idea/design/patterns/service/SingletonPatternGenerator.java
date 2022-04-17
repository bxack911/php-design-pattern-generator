package ore.plugins.idea.design.patterns.service;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import ore.plugins.idea.lib.service.JavaCodeGenerator;
import ore.plugins.idea.lib.utils.FormatUtils;

import java.util.Objects;

public class SingletonPatternGenerator extends JavaCodeGenerator {
    private static final String INSTANCE_FIELD_SUFFIX = "Instance";
    private static final String INSTANCE_METHOD_NAME = String.format("get%s", INSTANCE_FIELD_SUFFIX);

    public SingletonPatternGenerator(PhpClass psiClass) {
        super(psiClass);
    }

    @Override
    public void generateJavaClass() {
        String instanceFieldName = FormatUtils.toFirstLetterLowerCase(Objects.requireNonNull(psiClass.getName()).concat(INSTANCE_FIELD_SUFFIX));
        deleteRelated(psiClass, instanceFieldName);
        psiClass.addBefore(generateInstanceField(psiClass, instanceFieldName), psiClass.getLastChild());
        psiClass.addBefore(PhpPsiElementFactory.createWhiteSpace(psiClass.getProject()), psiClass.getLastChild());
        psiClass.addBefore(generatePrivateConstructor(psiClass), psiClass.getLastChild());
        psiClass.addBefore(generateInstanceMethod(psiClass, instanceFieldName), psiClass.getLastChild());
    }

    private void deleteRelated(PhpClass psiClass, String instanceFieldName) {
        if (psiClass.getConstructor() != null) {
            psiClass.getConstructor().delete();
        }
        psiClass.getFields().stream()
                .filter(member -> member.getName().equals(instanceFieldName))
                .forEach(PsiElement::delete);
        psiClass.getMethods().stream()
                .filter(member -> member.getName().equals(INSTANCE_METHOD_NAME))
                .forEach(PsiElement::delete);
    }

    private Method generatePrivateConstructor(PhpClass psiClass) {
        return PhpPsiElementFactory.createMethod(psiClass.getProject(), "private function " + PhpClass.CONSTRUCTOR + " (){ }");
    }

    private PhpPsiElement generateInstanceField(PhpClass psiClass, String instanceFieldName) {
        return PhpPsiElementFactory.createClassField(psiClass.getProject(), PhpModifier.PRIVATE_IMPLEMENTED_STATIC, instanceFieldName, null, "self");
    }

    private PhpPsiElement generateInstanceMethod(PhpClass psiClass, String instanceFieldName) {
        PhpPsiElement method = PhpPsiElementFactory.createMethod(psiClass.getProject(), "public static function " + INSTANCE_METHOD_NAME + " () { }");

        PhpPsiElement ifStatement = PhpPsiElementFactory.createPhpPsiFromText(psiClass.getProject(), If.class, "if (self::$" + instanceFieldName + " == null) { }");
        ifStatement.addBefore(PhpPsiElementFactory.createStatement(psiClass.getProject(), "self::$" + instanceFieldName + " = new " + psiClass.getName() + "();"), ifStatement.getLastChild().getLastChild());

        PsiElement closeBracket = method.getLastChild().getLastChild();
        method.addBefore(ifStatement, closeBracket);
        method.addBefore(PhpPsiElementFactory.createReturnStatement(psiClass.getProject(), "self::$" + instanceFieldName), closeBracket);
        return method;
    }
}
