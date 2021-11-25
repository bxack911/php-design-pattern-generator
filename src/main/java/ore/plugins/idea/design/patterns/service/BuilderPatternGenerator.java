package ore.plugins.idea.design.patterns.service;

import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import ore.plugins.idea.design.patterns.utils.PsiClassGeneratorUtils;
import ore.plugins.idea.design.patterns.wrapper.PsiModifierWrapper;
import ore.plugins.idea.lib.service.JavaCodeGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ore.plugins.idea.lib.utils.FormatUtils.toFirstLetterLowerCase;
import static ore.plugins.idea.lib.utils.FormatUtils.toFirstLetterUpperCase;

public class BuilderPatternGenerator extends JavaCodeGenerator {

    private static final String BUILDER_CLASS_NAME_SUFFIX = "Builder";

    private static final String BUILDER_ACCESS_METHOD_TEMPLATE = "/templates/creational/builder/builder-access-method";
    private static final String BUILDER_BUILD_METHOD_TEMPLATE = "/templates/creational/builder/build-method";

    private static final String BUILDER_BUILD_METHOD_SET_TEMPLATE = "%s.set%s(%s);";

    private static final String SETTER_METHOD_TEMPLATE = "public function set%s(%s %s): void {$this->%s = $s;}";
    private static final String GETTER_METHOD_TEMPLATE = "public function get%s(): %s {return $s;}";

    private List<Field> includedFields;
    private List<Field> mandatoryFields;
    private String builderClassName;

    public BuilderPatternGenerator(@NotNull PhpClass psiClass, List<Field> includedFields, List<Field> mandatoryFields) {
        super(psiClass);
        this.includedFields = includedFields;
        this.mandatoryFields = mandatoryFields;
        this.builderClassName = psiClass.getName().concat(BUILDER_CLASS_NAME_SUFFIX);
    }


    @Override
    public void generateJavaClass() {
        prepareParentClass();
        PhpClass innerBuilderClass = generateStuffForBuilderClass();
        psiClass.add(innerBuilderClass);
        //getJavaCodeStyleManager().shortenClassReferences(psiClass);
    }

    private void prepareParentClass() {
        Method constructor = generateConstructorForParentClass();
        List<Method> gettersAndSetters = generateGettersAndSettersForParentClass();
        deleteRelated(gettersAndSetters);
        //includedFields.forEach(PsiModifierWrapper.PRIVATE::applyModifier);
        psiClass.add(constructor);
        gettersAndSetters.forEach(psiClass::add);
    }

    private Method generateConstructorForParentClass() {
        String params = "";
        mandatoryFields.stream().map(field -> {
            String defaultVal = (field.getDefaultValue() != null) ? " = " . concat(field.getDefaultValue().getText()) : "";
            params.concat(field.getModifier() + " " + field.getName() + defaultVal);
            return field;
        });
        return PhpPsiElementFactory.createMethod(psiClass.getProject(), "private function " + PhpClass.CONSTRUCTOR + " (" + " + params + " + "){ }");
    }

    private List<Method> generateGettersAndSettersForParentClass() {
        List<Method> psiMethods = new ArrayList<>();

        includedFields.stream()
                .map(field -> {
                    psiMethods.add(PhpPsiElementFactory.createMethod(psiClass.getProject(), String.format(GETTER_METHOD_TEMPLATE,
                            toFirstLetterUpperCase(field.getName()),
                            field.getType(),
                            toFirstLetterLowerCase(field.getName())
                            )));
                    psiMethods.add(PhpPsiElementFactory.createMethod(psiClass.getProject(), String.format(SETTER_METHOD_TEMPLATE,
                            toFirstLetterUpperCase(field.getName()),
                            field.getType(),
                            toFirstLetterLowerCase(field.getName()),
                            toFirstLetterLowerCase(field.getName()),
                            toFirstLetterLowerCase(field.getName())
                    )));
                    return field;
                });
        return psiMethods;
    }

    private void deleteRelated(List<Method> gettersAndSetters) {
        /*Arrays.stream(psiClass.getInnerClasses())
                .filter(innerClass -> innerClass.getName() != null && innerClass.getName().equals(builderClassName))
                .forEach(PsiMember::delete);*/
        psiClass.getConstructor().delete();
        List<String> gettersAndSettersNames = gettersAndSetters.stream().map(Method::getName).collect(Collectors.toList());
        psiClass.getMethods().stream()
                .filter(parentClassMethod -> gettersAndSettersNames.contains(parentClassMethod.getName()))
                .forEach(Method::delete);
    }

    private PhpClass generateStuffForBuilderClass() {
        PhpClass builderClass = generateBuilderClass();
        generateBuilderFields().forEach(builderClass::add);
        builderClass.add(generateBuilderConstructor(builderClass));
        builderClass.add(generateBuilderAccessMethod());
        List<Field> includedFieldsWithoutMandatoryFields = includedFields.stream().filter(includedField -> !mandatoryFields.contains(includedField)).collect(Collectors.toList());
        generateBuilderWithMethods(builderClass, includedFieldsWithoutMandatoryFields).forEach(builderClass::add);
        builderClass.add(generateBuildMethod(builderClass, includedFieldsWithoutMandatoryFields));
        return builderClass;
    }

    private PhpClass generateBuilderClass() {
        PhpClass builderClass = PsiClassGeneratorUtils.generateClassForProjectWithName(psiClass.getProject(), builderClassName);
        PsiModifierWrapper.PUBLIC_STATIC.applyModifier(builderClass);
        return PhpPsiElementFactory.createClassEmptyConstructor(psiClass.getProject());
    }

    private List<Field> generateBuilderFields() {
        List<Field> builderFields = includedFields.stream()
                .map(includedField -> JavaPsiFacade.getElementFactory(psiClass.getProject()).createField(Objects.requireNonNull(includedField.getName()), includedField.getType()))
                .collect(Collectors.toList());
        builderFields.forEach(PsiModifierWrapper.PRIVATE::applyModifier);
        return builderFields;
    }

    private Method generateBuilderConstructor(PhpClass builderClass) {
        Method builderConstructor = PsiMemberGeneratorUtils.generateConstructorForClass(builderClass, mandatoryFields);
        PsiModifierWrapper.PRIVATE.applyModifier(builderConstructor);
        return builderConstructor;
    }

    private Method generateBuilderAccessMethod() {
        String argumentsWithTypes = PsiMemberGeneratorUtils.generateArgumentsWithTypesFromFields(mandatoryFields);
        String argumentsWithoutTypes = PsiMemberGeneratorUtils.generateArgumentsWithoutTypesFromFields(mandatoryFields);
        String builderAccessMethodContent = String.format(provideTemplateContent(BUILDER_ACCESS_METHOD_TEMPLATE), builderClassName, psiClass.getName(), argumentsWithTypes, builderClassName, argumentsWithoutTypes);
        PsiMethod builderAccessMethod = JavaPsiFacade.getElementFactory(psiClass.getProject()).createMethodFromText(builderAccessMethodContent, psiClass);
        PsiModifierWrapper.PUBLIC_STATIC.applyModifier(builderAccessMethod);
        return builderAccessMethod;
    }

    private Method generateBuildMethod(PhpClass builderClass, List<Field> includedFieldsWithoutMandatoryFields) {
        String parentClassName = Objects.requireNonNull(psiClass.getName());
        String lowercaseParentClassName = toFirstLetterLowerCase(parentClassName);
        String argumentsWithoutTypes = PsiMemberGeneratorUtils.generateArgumentsWithoutTypesFromFields(mandatoryFields);
        String setters = includedFieldsWithoutMandatoryFields.stream()
                .map(psiField -> String.format(BUILDER_BUILD_METHOD_SET_TEMPLATE, lowercaseParentClassName,
                        toFirstLetterUpperCase(Objects.requireNonNull(psiField.getName())),
                        toFirstLetterLowerCase(Objects.requireNonNull(psiField.getName()))))
                .collect(Collectors.joining("\n"));
        String buildMethodContent = String.format(provideTemplateContent(BUILDER_BUILD_METHOD_TEMPLATE),
                parentClassName, parentClassName, lowercaseParentClassName, parentClassName, argumentsWithoutTypes, setters, lowercaseParentClassName);
        PsiMethod builderMethod = JavaPsiFacade.getElementFactory(psiClass.getProject()).createMethodFromText(buildMethodContent, builderClass);
        PsiModifierWrapper.PUBLIC.applyModifier(builderMethod);
        return builderMethod;
    }

    private List<Method> generateBuilderWithMethods(PhpClass builderClass, List<Field> withFields) {
        return withFields.stream()
                .map(psiField -> {
                    String upperName = toFirstLetterUpperCase(Objects.requireNonNull(psiField.getName()));
                    String name = psiField.getName();
                    String type = psiField.getType().toString();
                    String content = String.format(provideTemplateContent("public function with%s(%s %s): %s"), upperName, type, name, builderClassName);
                    Method method = PhpPsiElementFactory.createMethod(builderClass.getProject(), content);
                    method.addBefore(PhpPsiElementFactory.createStatement(builderClass.getProject(), "$this->" + name + " = " + name), method.getLastChild().getLastChild());
                    method.addBefore(PhpPsiElementFactory.createReturnStatement(builderClass.getProject(), "$this"), method.getLastChild().getLastChild());
                    return method;
                })
                .collect(Collectors.toList());
    }
}