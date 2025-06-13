package cn.liziguo.scrcpy.constant;

/**
 * @author Liziguo
 * @date 2025-06-10
 */
public enum Codec {
    H264("h264"),
    H265("h265"),
    AV1("av1");

    private final String name;

    Codec(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
