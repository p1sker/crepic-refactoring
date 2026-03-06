package com.crepic.global.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "닉네임은 필수입니다.")
@Size(min = 2, max = 10, message = "{nickname.invalid}")
@Pattern(regexp = "^[가-힣a-zA-Z0-9]*$", message = "{nickname.invalid}")
public @interface Nickname {
    String message() default "{nickname.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}