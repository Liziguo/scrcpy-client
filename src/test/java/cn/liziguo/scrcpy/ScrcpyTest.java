package cn.liziguo.scrcpy;

import cn.liziguo.scrcpy.constant.Action;
import cn.liziguo.scrcpy.constant.KeyCode;
import cn.liziguo.scrcpy.constant.PowerMode;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;


/**
 * @author Liziguo
 * @date 2025-06-11
 */
public class ScrcpyTest extends JFrame {

    private final ScrcpyClient scrcpyClient;

    public ScrcpyTest() {
        super("ScrcpyTest");
        double rate = 0.5;
        setSize((int) (1080 * rate), (int) (1920 * rate));
        // 窗口居中
        setLocationRelativeTo(null);
        // x掉后彻底退出程序
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        MyCanvas canvas = new MyCanvas();
        add(canvas, BorderLayout.CENTER);


        List<String> devices = CommandUtil.abdDevices();
        System.out.println("当前已连接设备有: ");
        for (String device : devices) {
            System.out.println("\t" + device);
        }

        scrcpyClient = new ScrcpyClient();
        // scrcpyClient.setDevice("your-android-device");

        // 可选 无线连接(若无线调试无法连接，建议先通过有线方式连接一次，通常可自动恢复无线连接功能。)
        // scrcpyClient.setDevice("192.168.1.123:5555");

        // 自动连接一个设备
        if (scrcpyClient.getDevice() == null) {
            if (devices.isEmpty()) {
                throw new RuntimeException("请先连接手机或安卓模拟器");
            }
            scrcpyClient.setDevice(devices.get(0));
        }
        scrcpyClient.setOnFrame(canvas::drawImage);
        scrcpyClient.setMaxWidth(1080);
        scrcpyClient.setMaxFps(60);
        scrcpyClient.setBitrate(2_000_000);
//        scrcpyClient.setEncoder(Encoder.OMX_QCOM_VIDEO_ENCODER_AVC);
//        scrcpyClient.setEncoder(Encoder.OMX_GOOGLE_H264_ENCODER);
//        scrcpyClient.setEncoder(Encoder.C2_QTI_AVC_ENCODER);
//        scrcpyClient.setEncoder(Encoder.C2_ANDROID_AVC_ENCODER);
//        scrcpyClient.setCodec(Codec.AV1);
        System.out.println("\n即将连接设备: " + scrcpyClient.getDevice());
        scrcpyClient.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 27:
                        // Esc
                        System.out.println("按下返回键");
                        scrcpyClient.getControl().backOrTurnScreenOn(Action.DOWN);
                        break;
                    case 112:
                        // F1
                        System.out.println("按下Home键");
                        scrcpyClient.getControl().keycode(KeyCode.KEYCODE_HOME, Action.DOWN, 0);
                        break;
                    case 113:
                        // F2
                        System.out.println("关闭屏幕电源");
                        scrcpyClient.getControl().setScreenPowerMode(PowerMode.POWER_MODE_OFF);
                        break;
                    case 114:
                        // F3
                        System.out.println("打开屏幕电源");
                        scrcpyClient.getControl().setScreenPowerMode(PowerMode.POWER_MODE_NORMAL);
                        break;
                    case 115:
                        // F4
                        System.out.println("打开Android设备的下拉通知栏");
                        scrcpyClient.getControl().expandNotificationPanel();
                        break;
                    case 116:
                        // F5
                        System.out.println("打开Android设备的下拉菜单栏");
                        scrcpyClient.getControl().expandSettingsPanel();
                        break;
                    case 117:
                        // F6
                        System.out.println("收起Android设备的下拉通知栏或菜单栏");
                        scrcpyClient.getControl().collapsePanels();
                        break;
                    case 118:
                        // F7
                        System.out.println("读取剪切板数据: " + scrcpyClient.getControl().getClipboard(true));
                        break;
                    case 119:
                        // F8
                        double random = Math.random();
                        System.out.println("写入剪切板数据: " + random);
                        scrcpyClient.getControl().setClipboard(random + "", true);
                        break;
                    case 120:
                        // F9
                        System.out.println("旋转设备");
                        scrcpyClient.getControl().rotateDevice();
                        break;
                    case 121:
                        // F10
                        System.out.println("向Android中输入文本");
                        scrcpyClient.getControl().text("hhh");
                        break;
                    case 122:
                        // F11
                        System.out.println("打开键盘设置");
                        scrcpyClient.getControl().openHardKeyboardSettings();
                        break;
                    case 123:
                        // F12
                        System.out.println("启动微信");
                        scrcpyClient.getControl().startApp("com.tencent.mm");
                        break;
                    case 'W':
                        scrcpyClient.getControl().touch(0.5, 0.4, Action.DOWN, 2);
                        break;
                    case 'A':
                        scrcpyClient.getControl().touch(0.25, 0.5, Action.DOWN, 3);
                        break;
                    case 'S':
                        scrcpyClient.getControl().touch(0.5, 0.6, Action.DOWN, 4);
                        break;
                    case 'D':
                        scrcpyClient.getControl().touch(0.75, 0.5, Action.DOWN, 5);
                        break;
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 27:
                        // Esc
                        scrcpyClient.getControl().backOrTurnScreenOn(Action.UP);
                        break;
                    case 112:
                        // F1
                        scrcpyClient.getControl().keycode(KeyCode.KEYCODE_HOME, Action.UP, 0);
                        break;
                    case 'W':
                        scrcpyClient.getControl().touch(0.5, 0.4, Action.UP, 2);
                        break;
                    case 'A':
                        scrcpyClient.getControl().touch(0.25, 0.5, Action.UP, 3);
                        break;
                    case 'S':
                        scrcpyClient.getControl().touch(0.5, 0.6, Action.UP, 4);
                        break;
                    case 'D':
                        scrcpyClient.getControl().touch(0.75, 0.5, Action.UP, 5);
                        break;
                }
            }
        });
    }

    class MyCanvas extends JPanel {

        private final Java2DFrameConverter converter = new Java2DFrameConverter();
        private Frame frame;

        public MyCanvas() {
            setBackground(Color.BLACK);
            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    System.out.println("button: " + e.getButton());
                    if (frame == null) {
                        return;
                    }
                    float canvasWidth = getWidth();
                    float canvasHeight = getHeight();
                    float imageWidth = frame.imageWidth;
                    float imageHeight = frame.imageHeight;
                    float rate = Math.min(canvasWidth / imageWidth, canvasHeight / imageHeight);
                    float width = imageWidth * rate;
                    float height = imageHeight * rate;
                    int imageX = (int) ((canvasWidth - width) / 2);
                    int imageY = (int) ((canvasHeight - height) / 2);
                    float x = (e.getX() - imageX) / width;
                    float y = (e.getY() - imageY) / height;
                    scrcpyClient.getControl().touch(x, y, Action.DOWN, 1);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    System.out.println(e.getX() + ":" + e.getY());
                    if (frame == null) {
                        return;
                    }
                    float canvasWidth = getWidth();
                    float canvasHeight = getHeight();
                    float imageWidth = frame.imageWidth;
                    float imageHeight = frame.imageHeight;
                    float rate = Math.min(canvasWidth / imageWidth, canvasHeight / imageHeight);
                    float width = imageWidth * rate;
                    float height = imageHeight * rate;
                    int imageX = (int) ((canvasWidth - width) / 2);
                    int imageY = (int) ((canvasHeight - height) / 2);
                    float x = (e.getX() - imageX) / width;
                    float y = (e.getY() - imageY) / height;
                    scrcpyClient.getControl().touch(x, y, Action.UP, 1);
                }

            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (frame == null) {
                        return;
                    }
                    float canvasWidth = getWidth();
                    float canvasHeight = getHeight();
                    float imageWidth = frame.imageWidth;
                    float imageHeight = frame.imageHeight;
                    float rate = Math.min(canvasWidth / imageWidth, canvasHeight / imageHeight);
                    float width = imageWidth * rate;
                    float height = imageHeight * rate;
                    int imageX = (int) ((canvasWidth - width) / 2);
                    int imageY = (int) ((canvasHeight - height) / 2);
                    float x = (e.getX() - imageX) / width;
                    float y = (e.getY() - imageY) / height;
                    scrcpyClient.getControl().touch(x, y, Action.MOVE, 1);
                }
            });
            addMouseWheelListener(new MouseAdapter() {

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    Control control = scrcpyClient.getControl();
                    if (e.isShiftDown()) {
                        // 水平滚动（向右/向左）
                        int rotation = e.getWheelRotation();
                        if (rotation > 0) {
                            control.scroll(0.5, 0.5, 1, 0);
                        } else {
                            control.scroll(0.5, 0.5, -1, 0);
                        }
                    } else {
                        // 垂直滚动（向下/向上）
                        int rotation = e.getWheelRotation();
                        if (rotation > 0) {
                            control.scroll(0.5, 0.5, 0, 1);
                        } else {
                            control.scroll(0.5, 0.5, 0, -1);
                        }
                    }
                }
            });
        }

        public void drawImage(Frame frame) {
            this.frame = frame;
            repaint();
        }

        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            Graphics2D g = (Graphics2D) graphics;
            // 消除文字锯齿
            // g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // 消除画图锯齿
            // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 设置高质量插值算法（双三次插值）
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if (frame != null) {
                float canvasWidth = getWidth();
                float canvasHeight = getHeight();
                float imageWidth = frame.imageWidth;
                float imageHeight = frame.imageHeight;
                float rate = Math.min(canvasWidth / imageWidth, canvasHeight / imageHeight);
                float width = imageWidth * rate;
                float height = imageHeight * rate;
                BufferedImage image = converter.getBufferedImage(frame);
                g.drawImage(image, (int) ((canvasWidth - width) / 2), (int) ((canvasHeight - height) / 2), (int) width, (int) height, null);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new ScrcpyTest().setVisible(true);
    }
}
