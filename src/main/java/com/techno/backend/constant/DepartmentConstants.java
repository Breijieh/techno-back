package com.techno.backend.constant;

/**
 * Constants for Department entity
 */
public final class DepartmentConstants {

    /**
     * Department is active
     */
    public static final Character ACTIVE = 'Y';

    /**
     * Department is inactive
     */
    public static final Character INACTIVE = 'N';

    /**
     * Private constructor to prevent instantiation
     */
    private DepartmentConstants() {
        throw new UnsupportedOperationException("هذه فئة مساعدة ولا يمكن إنشاء مثيل منها");
    }
}
