package cn.liziguo.scrcpy;

import cn.liziguo.scrcpy.constant.Codec;
import cn.liziguo.scrcpy.constant.Encoder;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author Liziguo
 * @date 2025-06-10
 */
public class ScrcpyClient implements Closeable {

    static final String ADB_PATH = "adb.exe";
    static final String JAR_NAME = "scrcpy-server.jar";

    /**
     * 安卓设备 也可以是host:port的形式远程调试(如果远程调试连接不上可以尝试进行一次有线调试之后再远程调试就能连上了)
     */
    private String device;
    /**
     * 最大宽度：从安卓服务器广播的帧宽度
     */
    private int maxWidth;
    /**
     * 比特率
     */
    private int bitrate = 1024 * 1024 * 8;
    /**
     * 最大帧率，0 表示不限制（在安卓 10 之后支持）
     */
    private int maxFps;
    /**
     * 翻转视频
     */
    private boolean flip;
    /**
     * 块帧：仅返回非空帧
     */
    private boolean blockFrame;
    /**
     * 保持唤醒：保持安卓设备处于唤醒状态
     */
    private boolean stayAwake;
    /**
     * 锁定屏幕方向：锁定屏幕方向，LOCK_SCREEN_ORIENTATION_*
     */
    private int lockScreenOrientation;
    /**
     * 连接超时：连接超时时间，单位为毫秒
     */
    private int connectionTimeout = 3000;
    /**
     * 编码器名称：编码器名称，枚举值：[OMX.google.h264.encoder、OMX.qcom.video.encoder.avc、c2.qti.avc.encoder、c2.android.avc.encoder]
     */
    private Encoder encoder = Encoder.OMX_GOOGLE_H264_ENCODER;
    /**
     * 编码器名称：编码器名称，枚举值：[h264、h265、av1]
     */
    private Codec codec = Codec.H264;
    /**
     * 视频帧回调
     */
    private Consumer<Frame> onFrame;

    private StringBuilder startExceptionMsg;
    private String deviceName;
    private int resolutionWidth;
    private int resolutionHeight;
    private Socket videoSocket;
    private Socket controlSocket;
    private boolean alive;
    public Control control;

    public void start() {
        Objects.requireNonNull(device, "device must not be null");
        Objects.requireNonNull(encoder, "encoder must not be null");
        Objects.requireNonNull(codec, "codec must not be null");

        pushServer();
        Thread.ofVirtual().start(this::startServer);
        try {
            initServerConnection();
        } catch (IOException | InterruptedException e) {
            close();
            throw new RuntimeException(e);
        }
        Thread.ofPlatform().start(this::streamLoop);
    }

    void pushServer() {
        boolean connectHost = true;
        for (String d : CommandUtil.abdDevices()) {
            if (d.equals(device)) {
                connectHost = false;
                break;
            }
        }
        if (connectHost) {
            CommandUtil.cmdIgnore(ADB_PATH, "connect", device);
        }
        CommandUtil.cmdIgnore(ADB_PATH, "-s", device, "push", JAR_NAME, "/data/local/tmp/" + JAR_NAME);
    }

    void startServer() {
        String[] commands = {
                ADB_PATH,
                "-s",
                device,
                "shell",
                "CLASSPATH=/data/local/tmp/" + JAR_NAME,
                "app_process",
                "/",
                "com.genymobile.scrcpy.Server",
                "2.4",
                "log_level=info",
                "max_size=" + maxWidth,
                "max_fps=" + maxFps,
                "video_bit_rate=" + bitrate,
                "video_encoder=" + encoder.getName(),
                "video_codec=" + codec.getName(),
                "tunnel_forward=true",
                "send_frame_meta=false",
                "control=true",
                "audio=false",
                "show_touches=false",
                "stay_awake=false",
                "power_off_on_close=false",
                "clipboard_autosync=false",
        };
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                startExceptionMsg = new StringBuilder();
                if (!line.startsWith("[server] INFO:")) {
                    char[] buf = new char[1024];
                    int len;
                    while ((len = reader.read(buf)) != -1) {
                        startExceptionMsg.append(buf, 0, len);
                    }
                }
            }
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    void initServerConnection() throws IOException, InterruptedException {
        int videoPort = -1;
        for (int i = 0, maxRetryCount = 1 << 1 << 1 << 1 << 1 << 1 << 1; i < maxRetryCount; i++) {
            videoPort = getFreePort();
            String forward = CommandUtil.cmd(ADB_PATH, "-s", device, "forward", "tcp:" + videoPort, "localabstract:scrcpy");
            try {
                Integer.parseInt(forward.strip());
                break;
            } catch (NumberFormatException ignored) {
                videoPort = -1;
            }
        }
        if (videoPort < 0) {
            throw new RuntimeException("转发视频tcp连接失败 找不到可用的本机端口");
        }
        for (int i = 0, len = connectionTimeout / 100; i < len; i++) {
            Socket socket = new Socket("127.0.0.1", videoPort);
            // 关闭Nagle算法
            socket.setTcpNoDelay(true);
            // 开启TCP KeepAlive
            socket.setKeepAlive(true);
            InputStream inputStream = socket.getInputStream();
            int dummyByte = inputStream.read();
            // 有可能转发成功后scrcpy-server还没启动完成 导致这里读到的是-1
            if (dummyByte == 0) {
                videoSocket = socket;
                break;
            }
            socket.close();
            Thread.sleep(99);
        }
//        CommandUtil.cmd(ADB_PATH, "-s", device, "shell", "rm", "-f", "/data/local/tmp/" + JAR_NAME);
        if (videoSocket == null) {
            if (startExceptionMsg != null) {
                String msg = startExceptionMsg.toString();
                int start = 0;
                String prefix = "java.lang.IllegalArgumentException: ";
                if (msg.startsWith(prefix)) {
                    start = prefix.length();
                }
                throw new IllegalArgumentException(msg.substring(start).strip());
            }
            throw new RuntimeException(new TimeoutException("连接scrcpy-server超时"));
        }

        for (int i = 0, maxRetryCount = 1 << 1 << 1 << 1 << 1 << 1 << 1; i < maxRetryCount; i++) {
            int controlPort = getFreePort();
            String forward = CommandUtil.cmd(ADB_PATH, "-s", device, "forward", "tcp:" + controlPort, "localabstract:scrcpy");
            try {
                Integer.parseInt(forward.strip());
                controlSocket = new Socket("127.0.0.1", controlPort);
                // 关闭Nagle算法
                controlSocket.setTcpNoDelay(true);
                // 开启TCP KeepAlive
                controlSocket.setKeepAlive(true);
                break;
            } catch (NumberFormatException ignored) {
            }
        }
        if (controlSocket == null) {
            throw new RuntimeException("连接控制socket失败 找不到可以使用的端口");
        }

        InputStream inputStream = videoSocket.getInputStream();
        byte[] deviceNameBytes = inputStream.readNBytes(64);
        for (int i = 0; i < deviceNameBytes.length; i++) {
            if (deviceNameBytes[i] == 0x00) {
                deviceName = new String(deviceNameBytes, 0, i, StandardCharsets.UTF_8);
                break;
            }
        }
        if (deviceName == null) {
            throw new RuntimeException("未收到设备名称！");
        }

        byte[] resolutionBytes = inputStream.readNBytes(4);
        resolutionWidth = ((resolutionBytes[0] & 0XFF) << 8) | (resolutionBytes[1] & 0XFF);
        resolutionHeight = ((resolutionBytes[2] & 0XFF) << 8) | (resolutionBytes[3] & 0XFF);

        control = new Control(controlSocket, resolutionWidth, resolutionHeight);
    }

    void streamLoop() {
        alive = true;
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoSocket.getInputStream(), 0)) {
//            grabber.setFormat(codec.getName());
            grabber.setFormat("h264");

            // 核心低延迟参数
//            grabber.setOption("fflags", "nobuffer");       // 禁用输入缓冲
            grabber.setOption("flags", "low_delay");       // 全局低延迟模式
            grabber.setOption("tune", "zerolatency");      // 零延迟调优
            grabber.setOption("analyzeduration", "10");    // 减少流分析时间(ms)
            grabber.setOption("probesize", "32");          // 最小化探测数据量
//            grabber.setOption("buffer_size", "1024000"); // 1MB

            grabber.setOption("rtsp_transport", "tcp");    // TCP传输更稳定

            // 解码优化
            grabber.setOption("avioflags", "direct");      // 减少缓冲
            grabber.setVideoOption("threads", "1");         // 单线程解码(避免并行开销) 核心 设置这个之后速度快很多

            grabber.setOption("hwaccel", "auto");

            grabber.start();

            // 2.创建一个帧-->图片的转换器
            while (alive) {
                Frame frame = grabber.grabFrame(false, true, true, false, true);
                if (frame.image == null) {
                    continue;
                }
                control.resolutionWidth = frame.imageWidth;
                control.resolutionHeight = frame.imageHeight;

                if (onFrame != null) {
                    onFrame.accept(frame.clone());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        alive = false;
        if (videoSocket != null) {
            try {
                videoSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (controlSocket != null) {
            try {
                controlSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getMaxFps() {
        return maxFps;
    }

    public void setMaxFps(int maxFps) {
        this.maxFps = maxFps;
    }

    public boolean isFlip() {
        return flip;
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
    }

    public boolean isBlockFrame() {
        return blockFrame;
    }

    public void setBlockFrame(boolean blockFrame) {
        this.blockFrame = blockFrame;
    }

    public boolean isStayAwake() {
        return stayAwake;
    }

    public void setStayAwake(boolean stayAwake) {
        this.stayAwake = stayAwake;
    }

    public int getLockScreenOrientation() {
        return lockScreenOrientation;
    }

    public void setLockScreenOrientation(int lockScreenOrientation) {
        this.lockScreenOrientation = lockScreenOrientation;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public Codec getCodec() {
        return codec;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    public Consumer<Frame> getOnFrame() {
        return onFrame;
    }

    public void setOnFrame(Consumer<Frame> onFrame) {
        this.onFrame = onFrame;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getResolutionWidth() {
        return resolutionWidth;
    }

    public int getResolutionHeight() {
        return resolutionHeight;
    }

    public Socket getVideoSocket() {
        return videoSocket;
    }

    public Socket getControlSocket() {
        return controlSocket;
    }

    public boolean isAlive() {
        return alive;
    }

    public Control getControl() {
        return control;
    }

    static int getFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
