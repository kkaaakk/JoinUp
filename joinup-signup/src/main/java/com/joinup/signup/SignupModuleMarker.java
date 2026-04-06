package com.joinup.signup;

/**
 * 报名模块标记类。
 * <p>
 * 该类本身不承载任何业务逻辑，主要用作模块边界的类型锚点。
 * 例如在自动配置、包扫描、模块依赖引用或测试装配时，可以通过这个类来表达
 * “当前引用的是报名模块本身”，而不需要依赖某个具体业务类。
 * </p>
 */
public final class SignupModuleMarker {

    /**
     * 私有构造方法。
     * <p>
     * 标记类不应该被实例化，因此显式隐藏构造方法，避免误用。
     * </p>
     */
    private SignupModuleMarker() {
    }
}
