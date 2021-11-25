package ore.plugins.idea.design.patterns.utils;

import com.intellij.psi.PsiClass;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import ore.plugins.idea.design.patterns.exception.DuplicateNameException;
import ore.plugins.idea.design.patterns.exception.InvalidNameException;

import java.util.Arrays;

public interface ClassNameValidator {
    String PHP_FILE_EXTENSION = ".php";

    default String validateClassNameOrThrow(PhpClass psiClass, String selectedName) {
        makeSureSelectedNameIsNotEmpty(selectedName);
        makeSureFileDoesNotExist(psiClass, selectedName.concat(PHP_FILE_EXTENSION));
        return selectedName;
    }

    default void makeSureSelectedNameIsNotEmpty(String selectedName) {
        if (selectedName == null || selectedName.length() <= 0) throw new InvalidNameException(selectedName);
    }

    default void makeSureFileDoesNotExist(PhpClass psiClass, String fileName) {
        if (Arrays.stream(psiClass.getContainingFile().getContainingDirectory().getFiles())
                .anyMatch(psiFile -> psiFile.getName().equals(fileName))) throw new DuplicateNameException(fileName);
    }
}
