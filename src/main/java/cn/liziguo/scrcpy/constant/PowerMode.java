package cn.liziguo.scrcpy.constant;

/**
 * @author Liziguo
 * @date 2025-06-11
 */
public enum PowerMode {
    POWER_MODE_OFF((byte) 0),
    POWER_MODE_NORMAL((byte) 2);

    private final byte code;

    PowerMode(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
