package ic2.compat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Test metadata consumed by Ic2GameTests to register the existing suite on 26.3. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
  String template();

  int timeoutTicks() default 100;

  int setupTicks() default 0;

  String batch() default "default";

  boolean required() default true;
}
