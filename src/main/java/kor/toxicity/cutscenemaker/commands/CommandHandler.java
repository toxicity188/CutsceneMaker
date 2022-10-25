package kor.toxicity.cutscenemaker.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandHandler {

    String[] aliases() default {};
    String description();
    String usage();
    int length();
    boolean opOnly() default true;
    SenderType[] sender() default SenderType.PLAYER;

}
