/*
 * Decompiled with CFR 0.152.
 */
package com.temporedata.guard.modules.security.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Audit {
    public String action() default "OPERATION";

    public String targetType() default "";

    public String targetName() default "";

    public String detail() default "";
}

