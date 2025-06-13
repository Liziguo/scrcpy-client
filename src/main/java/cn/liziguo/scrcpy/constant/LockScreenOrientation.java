package cn.liziguo.scrcpy.constant;

/**
 * @author Liziguo
 * @date 2025-06-12
 */
public enum LockScreenOrientation {

    LOCK_SCREEN_ORIENTATION_UNLOCKED(-1),
    LOCK_SCREEN_ORIENTATION_INITIAL(-2),
    LOCK_SCREEN_ORIENTATION_0(0),
    LOCK_SCREEN_ORIENTATION_1(1),
    LOCK_SCREEN_ORIENTATION_2(2),
    LOCK_SCREEN_ORIENTATION_3(3);

    private final int code;

    LockScreenOrientation(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
