package ore.plugins.idea.design.patterns.service;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import ore.plugins.idea.design.patterns.utils.PhpPsiClassGeneratorUtils;
import ore.plugins.idea.lib.service.JavaCodeGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ore.plugins.idea.lib.utils.FormatUtils.toFirstLetterLowerCase;
import static ore.plugins.idea.lib.utils.FormatUtils.toFirstLetterUpperCase;

public class BuilderPatternGenerator extends JavaCodeGenerator {

    private static final String BUILDER_CLASS_NAME_SUFFIX = "Builder";

    private static final String BUILDER_BUILD_METHOD_SET_TEMPLATE = "$%s->set%s($this->%s);";

    private static final String SETTER_METHOD_TEMPLATE = "public function set%s(%s $%s): self {$this->%s = $%s;\nreturn $this;}";
    private static final String GETTER_METHOD_TEMPLATE = "public function get%s()%s {return $this->%s;}";

    private List<Field> includedFields;
    private List<Field> mandatoryFields;
    private String builderClassName;
    private String psiPackageStatement;

    public BuilderPatternGenerator(@NotNull PhpClass psiClass, List<Field> includedFields, List<Field> mandatoryFields) {
        super(psiClass);
        this.includedFields = includedFields;
        this.mandatoryFields = mandatoryFields;
        this.builderClassName = psiClass.getName().concat(BUILDER_CLASS_NAME_SUFFIX);
        this.psiPackageStatement = psiClass.getNamespaceName().replace("\\", "");
    }


    @Override
    public void generateJavaClass() {
        prepareParentClass();
        generateStuffForBuilderClass();
        //psiClass.add(innerBuilderClass);
        //getJavaCodeStyleManager().shortenClassReferences(psiClass);
    }

    private void prepareParentClass() {
        Method constructor = generateConstructorForParentClass();
        List<Method> gettersAndSetters = generateGettersAndSettersForParentClass();
        deleteRelated(gettersAndSetters);
        //includedFields.forEach(PsiModifierWrapper.PRIVATE::applyModifier);
        psiClass.addBefore(constructor, psiClass.getLastChild());

        for (Method method : gettersAndSetters) {
            psiClass.addBefore(method, psiClass.getLastChild());
        }
    }

    private Method generateConstructorForParentClass() {
        String parameters = mandatoryFields.stream()
                .map(field -> {
                    String defaultVal = (field.getDefaultValue() != null) ? " = " . concat(field.getDefaultValue().getText()) : "";
                    return (!Objects.equals(field.getType().toString(), "") ? field.getType() + " " : "") + "$" + field.getName() + defaultVal;
                })
                .collect(Collectors.joining(", "));

        String assigns = mandatoryFields.stream()
                .map(field -> {
                    return "$this->" + field.getName() + " = $" + field.getName() + ";";
                })
                .collect(Collectors.joining("\n"));

        return PhpPsiElementFactory.createMethod(psiClass.getProject(), "public function " + PhpClass.CONSTRUCTOR + " (" +  parameters + "){ " + assigns + " }");
    }

    private List<Method> generateGettersAndSettersForParentClass() {
        List<Method> gettersMethods = includedFields.stream()
                .map(field -> PhpPsiElementFactory.createMethod(psiClass.getProject(), String.format(GETTER_METHOD_TEMPLATE,
                        toFirstLetterUpperCase(field.getName()),
                        (!Objects.equals(field.getType().toString(), "")) ? ": " + field.getType() : "",
                        toFirstLetterLowerCase(field.getName())
                        )))
                .collect(Collectors.toList());


        List<Method> settersMethods = includedFields.stream()
                .map(field -> PhpPsiElementFactory.createMethod(psiClass.getProject(), String.format(SETTER_METHOD_TEMPLATE,
                        toFirstLetterUpperCase(field.getName()),
                        field.getType(),
                        toFirstLetterLowerCase(field.getName()),
                        toFirstLetterLowerCase(field.getName()),
                        toFirstLetterLowerCase(field.getName())
                )))
                .collect(Collectors.toList());

        return Stream.concat(gettersMethods.stream(), settersMethods.stream())
                .collect(Collectors.toList());
    }

    private void deleteRelated(List<Method> gettersAndSetters) {
        /*Arrays.stream(psiClass.getInnerClasses())
                .filter(innerClass -> innerClass.getName() != null && innerClass.getName().equals(builderClassName))
                .forEach(PsiMember::delete);*/
        //psiClass.getConstructor().delete();
        List<String> gettersAndSettersNames = gettersAndSetters.stream().map(Method::getName).collect(Collectors.toList());
        psiClass.getMethods().stream()
                .filter(parentClassMethod -> gettersAndSettersNames.contains(parentClassMethod.getName()))
                .forEach(Method::delete);
    }

    private PhpClass generateStuffForBuilderClass() {
        PhpClass builderClass = (new PhpPsiClassGeneratorUtils(psiClass, this.psiPackageStatement, builderClassName)).generateClass(Collections.emptyList());
        generateBuilderFields(builderClass);
        builderClass.addBefore(generateBuilderConstructor(), builderClass.getLastChild());
        builderClass.addBefore(generateBuilderAccessMethod(), builderClass.getLastChild());
        List<Field> includedFieldsWithoutMandatoryFields = includedFields.stream().filter(includedField -> !mandatoryFields.contains(includedField)).collect(Collectors.toList());
        generateBuilderWithMethods(builderClass, includedFieldsWithoutMandatoryFields);
        builderClass.addBefore(generateBuildMethod(builderClass, includedFieldsWithoutMandatoryFields), builderClass.getLastChild());
        return builderClass;
    }

    private PhpClass generateBuilderFields(PhpClass builderClass) {
        List<PhpPsiElement> builderFields = includedFields.stream()
                .map(includedField -> {
                    return PhpPsiElementFactory.createClassField(psiClass.getProject(), PhpModifier.PRIVATE_IMPLEMENTED_DYNAMIC, includedField.getName(), null, includedField.getType().toString());
                })
                .collect(Collectors.toList());

        for (PhpPsiElement field : builderFields) {
            builderClass.addBefore(field, builderClass.getLastChild());
        }

        return builderClass;
    }

    private Method generateBuilderConstructor() {
        StringBuilder methodSb = new StringBuilder();
        methodSb.append("private function __construct(");
        String parameters = mandatoryFields.stream()
                .map(psiParameter -> psiParameter.getType() + " $" + psiParameter.getName())
                .collect(Collectors.joining(", "));
        methodSb.append(parameters).append(") {}");
        return PhpPsiElementFactory.createMethod(psiClass.getProject(), methodSb.toString());
    }

    private Method generateBuilderAccessMethod() {
        StringBuilder methodSb = new StringBuilder();
        methodSb.append("public function " + toFirstLetterLowerCase(builderClassName) + "(");
        String argumentsWithTypes = mandatoryFields.stream()
                .map(psiParameter -> psiParameter.getType() + " $" + psiParameter.getName())
                .collect(Collectors.joining(", "));

        String argumentsWithoutTypes = mandatoryFields.stream()
                .map(psiParameter -> "$" + psiParameter.getName())
                .collect(Collectors.joining(", "));

        methodSb.append(argumentsWithTypes).append(") { return new self(").append(argumentsWithoutTypes).append("); }");

        return PhpPsiElementFactory.createMethod(psiClass.getProject(), methodSb.toString());
    }

    private void generateBuilderWithMethods(PhpClass builderClass, List<Field> withFields) {
        for (Field psiField : withFields) {
            String upperName = toFirstLetterUpperCase(Objects.requireNonNull(psiField.getName()));
            String name = psiField.getName();
            String content = "public function with" + upperName + "(" + psiField.getType() + "$" + name + "): self {$this->" + name + " = $" + name + ";\nreturn $this;}";
            builderClass.addBefore(PhpPsiElementFactory.createMethod(builderClass.getProject(), content), builderClass.getLastChild());
        }
    }

    private Method generateBuildMethod(PhpClass builderClass, List<Field> includedFieldsWithoutMandatoryFields) {
        String parentClassName = Objects.requireNonNull(psiClass.getName());
        String lowercaseParentClassName = toFirstLetterLowerCase(parentClassName);

        String argumentsWithoutTypes = mandatoryFields.stream()
                .map(argument -> "$this->" + argument.getName())
                .collect(Collectors.joining(", "));

        StringBuilder methodSb = new StringBuilder();
        methodSb.append("private function build(){");
        methodSb.append("$" + lowercaseParentClassName + " = new " + parentClassName + "(" + argumentsWithoutTypes + ");");
        methodSb.append("\n");

        for (Field field : includedFieldsWithoutMandatoryFields) {
            methodSb.append(String.format(BUILDER_BUILD_METHOD_SET_TEMPLATE, lowercaseParentClassName,
                    toFirstLetterUpperCase(Objects.requireNonNull(field.getName())),
                    toFirstLetterLowerCase(Objects.requireNonNull(field.getName()))));
            methodSb.append("\n");
        }

        methodSb.append("return new self(" + argumentsWithoutTypes + ");");

        return PhpPsiElementFactory.createMethod(builderClass.getProject(), methodSb.toString());
    }
}