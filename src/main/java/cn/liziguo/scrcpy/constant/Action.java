package cn.liziguo.scrcpy.constant;

/**
 * @author Liziguo
 * @date 2025-06-11
 */
public enum Action {
    /**
     * 按下
     */
    DOWN((byte) 0),
    /**
     * 松开
     */
    UP((byte) 1),
    /**
     * 移动
     */
    MOVE((byte) 2);

    private final byte code;

    Action(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
