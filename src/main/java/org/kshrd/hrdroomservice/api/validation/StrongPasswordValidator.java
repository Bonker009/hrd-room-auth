package org.kshrd.hrdroomservice.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final PasswordValidator PASSWORD_VALIDATOR =
            new PasswordValidator(
                    Arrays.asList(
                            new LengthRule(8, 128),
                            new CharacterRule(EnglishCharacterData.UpperCase, 1),
                            new CharacterRule(EnglishCharacterData.LowerCase, 1),
                            new CharacterRule(EnglishCharacterData.Digit, 1),
                            new CharacterRule(EnglishCharacterData.Special, 1),
                            new WhitespaceRule()));

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true;
        }

        RuleResult result = PASSWORD_VALIDATOR.validate(new PasswordData(password));
        if (result.isValid()) {
            return true;
        }

        List<String> messages = PASSWORD_VALIDATOR.getMessages(result);
        String template = messages.isEmpty() ? "Invalid password" : messages.get(0);

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(template).addConstraintViolation();
        return false;
    }
}
