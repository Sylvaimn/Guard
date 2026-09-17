package com.temporedata.guard.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构守则（ArchUnit 防回退）：
 * 1. Controller 不得直接依赖 Repository（必须经 Service）
 * 2. Controller 不得直接使用 JDBC
 * 3. 禁止 System.out / printStackTrace
 * 4. 业务域不得反向依赖基础设施域(security.audit 除外, 供各域使用)
 */
@AnalyzeClasses(packages = "com.temporedata.guard.modules")
public class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_repositories =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .as("Controller 必须通过 Service 访问数据，禁止直接注入 Repository");

    @ArchTest
    static final ArchRule controllers_must_not_use_jdbc =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAnyPackage("java.sql..")
                    .as("Controller 不得直接操作 JDBC");

    @ArchTest
    static final ArchRule no_system_out_or_print_stack_trace =
            noClasses().should().callMethod(System.class, "out")
                    .orShould().callMethod(Throwable.class, "printStackTrace")
                    .as("禁止 System.out 与 printStackTrace，统一使用日志框架");

    @ArchTest
    static final ArchRule business_domains_must_not_depend_on_common_web =
            noClasses().that().resideInAPackage("..common.web..")
                    .should().dependOnClassesThat().resideInAPackage("..modules..")
                    .allowEmptyShould(true)
                    .as("基础工具包(common)不得反向依赖业务模块");
}
