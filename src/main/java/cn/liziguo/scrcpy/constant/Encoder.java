package cn.liziguo.scrcpy.constant;

/**
 * @author Liziguo
 * @date 2025-06-10
 */
public enum Encoder {
    OMX_GOOGLE_H264_ENCODER("OMX.google.h264.encoder"),
    OMX_QCOM_VIDEO_ENCODER_AVC("OMX.qcom.video.encoder.avc"),
    C2_QTI_AVC_ENCODER("c2.qti.avc.encoder"),
    C2_ANDROID_AVC_ENCODER("c2.android.avc.encoder");

    private final String name;

    Encoder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
