package com.example.hospital_management_system.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AppointmentSlotValidator.class)
public @interface ValidAppointmentSlot {

    String message() default "Appointment time falls outside the doctor's scheduled availability";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
