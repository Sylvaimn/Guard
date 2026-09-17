/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.security.LoginUser
 *  javax.servlet.http.HttpServletRequest
 *  org.aspectj.lang.ProceedingJoinPoint
 *  org.aspectj.lang.annotation.Around
 *  org.aspectj.lang.annotation.Aspect
 *  org.aspectj.lang.reflect.MethodSignature
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.core.DefaultParameterNameDiscoverer
 *  org.springframework.core.ParameterNameDiscoverer
 *  org.springframework.expression.EvaluationContext
 *  org.springframework.expression.ExpressionParser
 *  org.springframework.expression.ParserContext
 *  org.springframework.expression.common.TemplateParserContext
 *  org.springframework.expression.spel.standard.SpelExpressionParser
 *  org.springframework.expression.spel.support.StandardEvaluationContext
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.core.context.SecurityContextHolder
 *  org.springframework.stereotype.Component
 *  org.springframework.web.context.request.RequestContextHolder
 *  org.springframework.web.context.request.ServletRequestAttributes
 */
package com.temporedata.guard.modules.security.security.audit;


import com.temporedata.guard.modules.security.security.service.AuditLogService;
import com.temporedata.guard.security.LoginUser;
import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final AuditLogService auditLogService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final TemplateParserContext templateContext = new TemplateParserContext();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around(value="@annotation(audit)")
    public Object audit(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();
        String errorMsg = null;
        String resultStatus = "SUCCESS";
        try {
            Object result;
            Object object = result = pjp.proceed();
            return object;
        }
        catch (Throwable e) {
            resultStatus = "FAILED";
            errorMsg = e.getMessage() != null && e.getMessage().length() > 200 ? e.getMessage().substring(0, 200) : e.getMessage();
            throw e;
        }
        finally {
            try {
                this.record(pjp, audit, resultStatus, errorMsg, System.currentTimeMillis() - start);
            }
            catch (Exception ex) {
                log.warn("Audit record failed: {}", (Object)ex.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint pjp, Audit audit, String result, String errorMsg, long costMs) {
        int i;
        String username = "SYSTEM";
        String tenantId = "DEFAULT";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            LoginUser user = (LoginUser)auth.getPrincipal();
            username = user.getUsername();
            tenantId = user.getTenantId();
        }
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Object[] args = pjp.getArgs();
        String[] paramNames = this.parameterNameDiscoverer.getParameterNames(((MethodSignature)pjp.getSignature()).getMethod());
        if (paramNames != null) {
            for (i = 0; i < paramNames.length && i < args.length; ++i) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        } else {
            for (i = 0; i < args.length; ++i) {
                ctx.setVariable("arg" + i, args[i]);
            }
        }
        String targetName = this.eval(ctx, audit.targetName());
        Object detail = this.eval(ctx, audit.detail());
        if (costMs > 0L) {
            detail = (String)(detail == null || ((String)detail).isEmpty() ? "" : (String)detail + "; ") + "\u8017\u65f6" + costMs + "ms";
        }
        this.auditLogService.record(username, audit.action(), (String)detail, audit.targetType(), targetName, this.resolveIp(), result, errorMsg, tenantId);
    }

    private String eval(StandardEvaluationContext ctx, String expr) {
        if (expr == null || expr.isBlank()) {
            return null;
        }
        try {
            Object value = this.parser.parseExpression(expr, (ParserContext)this.templateContext).getValue((EvaluationContext)ctx);
            return value == null ? null : String.valueOf(value);
        }
        catch (Exception e) {
            log.warn("Audit SpEL eval failed: {} -> {}", (Object)expr, (Object)e.getMessage());
            return expr;
        }
    }

    private String resolveIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "";
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
}

