package com.joinup.credit;

/**
 * 信用模块标记类。
 * <p>
 * 这个类本身不承载业务逻辑，主要用于模块扫描、自动配置锚点和后续测试装配。
 * 当其他模块只需要表达“依赖的是信用模块”而不是某个具体业务类时，可以直接引用这个标记类。
 * </p>
 */
public final class CreditModuleMarker {

    /**
     * 私有构造方法。
     * <p>
     * 标记类不应该被实例化，因此显式隐藏构造方法，避免误用。
     * </p>
     */
    private CreditModuleMarker() {
    }
}