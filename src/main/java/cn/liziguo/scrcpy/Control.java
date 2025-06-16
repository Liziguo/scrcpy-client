package cn.liziguo.scrcpy;

import cn.liziguo.scrcpy.constant.*;
import cn.liziguo.scrcpy.exception.ControlException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author Liziguo
 * @date 2025-06-11
 */
public class Control {
    private final DataInputStream in;
    private final DataOutputStream out;
    /**
     * 上一帧的宽度
     */
    int resolutionWidth;
    /**
     * 上一帧的高度
     */
    int resolutionHeight;

    Control(Socket socket, int resolutionWidth, int resolutionHeight) throws IOException {
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
    }

    /**
     * 设置 Android 设备的屏幕电源模式。
     *
     * <p>模式说明：
     * <ul>
     *   <li><b>POWER_MODE_NORMAL（正常模式）</b>：屏幕正常显示，完全供电状态</li>
     *   <li><b>POWER_MODE_OFF（关闭模式）</b>：屏幕电源关闭但设备未完全灭屏（区别于完全灭屏状态）</li>
     * </ul>
     *
     * <p>关闭模式的特点：
     * <ul>
     *   <li>显著降低设备在投屏期间的功耗</li>
     *   <li>投屏端仍可正常显示和操作设备</li>
     *   <li>适用于需要长时间投屏但希望节省电量的场景</li>
     * </ul>
     *
     * @param mode 要设置的电源模式（不能为 null）
     * @throws ControlException 如果发生 I/O 错误，包装原始 IOException 抛出
     * @see PowerMode
     */
    public synchronized void setScreenPowerMode(PowerMode mode) {
        try {
            out.write(ControlType.TYPE_SET_SCREEN_POWER_MODE);
            out.write(mode.getCode());
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    /**
     * 向 Android 设备发送按键事件。
     *
     * <p>此方法通过 ADB 协议发送按键指令到设备，支持完整的按键按下-抬起事件序列。
     *
     * <p><b>使用示例：</b>
     * <pre>{@code
     * // 模拟按下并释放返回键
     * control.keycode(KeyCode.KEYCODE_BACK, Action.DOWN, 0);  // 按下
     * control.keycode(KeyCode.KEYCODE_BACK, Action.UP, 0);    // 释放
     * }</pre>
     *
     * @param keycode 按键码，必须使用 {@link KeyCode} 中定义的常量值
     * @param action  按键动作，{@link Action#DOWN} 表示按下，{@link Action#UP} 表示释放
     * @param repeat  事件重复次数（0=不重复，n=连续触发n次）
     * @throws ControlException 如果发生 I/O 错误，包装原始 IOException 抛出
     * @see KeyCode
     * @see Action
     */
    public synchronized void keycode(int keycode, Action action, int repeat) {
        try {
            out.write(ControlType.TYPE_INJECT_KEYCODE);
            out.write(action.getCode());
            out.writeInt(keycode);
            out.writeInt(repeat);
            out.writeInt(0);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    /**
     * 向当前获得焦点的Android输入框中注入文本内容。
     *
     * <p><b>前提条件：</b>
     * <ul>
     *   <li>目标Android设备上必须有处于焦点状态的输入框</li>
     *   <li>输入法必须处于可接收文本输入的状态</li>
     * </ul>
     *
     *
     * <p><b>示例：</b>
     * <pre>{@code
     * // 在焦点输入框中输入"Hello World"
     * control.text("Hello World");
     * }</pre>
     *
     * @param text 要输入的文本内容（不能为null，空字符串是允许的）
     * @throws ControlException     如果发生I/O错误或文本编码转换失败
     * @throws NullPointerException 如果text参数为null
     * @see StandardCharsets#UTF_8
     */
    public synchronized void text(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        try {
            out.write(ControlType.TYPE_INJECT_TEXT);
            out.writeInt(length);
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    /**
     * 向Android设备发送触摸事件，支持多点触控操作。
     *
     * <p><b>坐标系统：</b>
     * <ul>
     *   <li>使用归一化坐标系统(0.0-1.0)，其中：
     *     <ul>
     *       <li>(0.0, 0.0)表示屏幕左上角</li>
     *       <li>(1.0, 1.0)表示屏幕右下角</li>
     *     </ul>
     *   </li>
     *   <li>支持超出[0.0,1.0]范围的坐标值（可用于特殊场景）</li>
     *   <li>内部会自动将归一化坐标转换为设备物理分辨率</li>
     * </ul>
     *
     * <p><b>多点触控：</b>
     * <ul>
     *   <li>通过{@code touchId}区分不同触控点</li>
     *   <li>同时使用不同的{@code touchId}可实现真正的多点触控</li>
     *   <li>同一{@code touchId}的连续操作会被视为同一触控点的操作序列</li>
     * </ul>
     *
     * <p><b>事件类型：</b>
     * <ul>
     *   <li>{@link Action#DOWN} - 触控点按下</li>
     *   <li>{@link Action#UP} - 触控点抬起</li>
     *   <li>{@link Action#MOVE} - 触控点移动</li>
     * </ul>
     *
     * <p><b>使用示例：</b>
     * <pre>{@code
     * // 单点触控：在屏幕中心按下并抬起
     * control.touch(0.5, 0.5, Action.DOWN, 0);  // 按下
     * control.touch(0.5, 0.5, Action.UP, 0);    // 抬起
     *
     * // 两点触控：同时操作两个不同位置
     * control.touch(0.3, 0.3, Action.DOWN, 1);  // 第一个触控点
     * control.touch(0.7, 0.7, Action.DOWN, 2);  // 第二个触控点
     * // ...后续操作...
     * }</pre>
     *
     * @param x       触摸点x坐标(0.0=最左侧，1.0=最右侧，支持超出此范围的值)
     * @param y       触摸点y坐标(0.0=最上侧，1.0=最下侧，支持超出此范围的值)
     * @param action  触摸动作类型(不可为null)
     * @param touchId 触控点唯一标识符(相同ID表示同一触控点)
     * @throws ControlException     当发生I/O错误时抛出
     * @throws NullPointerException 当action参数为null时抛出
     * @see Action
     */
    public synchronized void touch(double x, double y, Action action, long touchId) {
        try {
            out.write(ControlType.TYPE_INJECT_TOUCH_EVENT);
            out.write(action.getCode());
            out.writeLong(touchId);
            out.writeInt((int) (x * resolutionWidth));
            out.writeInt((int) (y * resolutionHeight));
            out.writeShort(resolutionWidth);
            out.writeShort(resolutionHeight);
            // 压力 0-65535
            out.writeShort(0xFFFF);
            out.writeInt(AndroidMotionEventToolType.AMOTION_EVENT_TOOL_TYPE_FINGER);
            out.writeInt(AndroidMotionEventToolType.AMOTION_EVENT_TOOL_TYPE_FINGER);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    short floatToI16FixedPoint(double value) {
        // 钳制输入到合法范围 [-1.0, 1.0]
        if (value > 1.0f) {
            value = 1.0;
        } else if (value < -1.0) {
            value = -1.0;
        }
        value *= 32768.0;
        // 四舍五入到长整型
        long rounded = Math.round(value);

        // 钳制到short的范围
        if (rounded > Short.MAX_VALUE) {
            rounded = Short.MAX_VALUE;
        } else if (rounded < Short.MIN_VALUE) {
            rounded = Short.MIN_VALUE;
        }
        return (short) rounded;
    }

    /**
     * 向Android设备发送滚动事件，支持水平和垂直方向的滚动操作。
     *
     * <p><b>坐标系统：</b>
     * <ul>
     *   <li>使用归一化坐标系统(0.0-1.0)，其中：
     *     <ul>
     *       <li>(0.0, 0.0)表示屏幕左上角</li>
     *       <li>(1.0, 1.0)表示屏幕右下角</li>
     *     </ul>
     *   </li>
     *   <li>内部会自动将归一化坐标转换为设备物理分辨率</li>
     * </ul>
     *
     * <p><b>滚动参数：</b>
     * <ul>
     *   <li>滚动距离使用与坐标相同的归一化系统</li>
     *   <li>正值表示向右/向下滚动，负值表示向左/向上滚动</li>
     *   <li>滚动距离会乘以屏幕分辨率转换为实际像素值</li>
     * </ul>
     *
     * <p><b>使用示例：</b>
     * <pre>{@code
     * // 从屏幕中心向右滚动屏幕宽度的10%
     * control.scroll(0.5, 0.5, 0.1, 0);
     *
     * // 从(0.2,0.2)位置向上滚动屏幕高度的20%
     * control.scroll(0.2, 0.2, 0, -0.2);
     * }</pre>
     *
     * @param x 滚动起始点的x坐标(0.0=最左侧，1.0=最右侧)
     * @param y 滚动起始点的y坐标(0.0=最上侧，1.0=最下侧)
     * @param h 水平滚动距离(正值=向右，负值=向左)
     * @param v 垂直滚动距离(正值=向下，负值=向上)
     * @throws ControlException 当发生I/O错误时抛出
     */
    public synchronized void scroll(double x, double y, double h, double v) {
        try {
            out.write(ControlType.TYPE_INJECT_SCROLL_EVENT);
            out.writeInt((int) (x * resolutionWidth));
            out.writeInt((int) (y * resolutionHeight));
            out.writeShort(resolutionWidth);
            out.writeShort(resolutionHeight);
            out.writeShort(floatToI16FixedPoint(h));
            out.writeShort(floatToI16FixedPoint(v));
            out.writeInt(AndroidMotionEventToolType.AMOTION_EVENT_TOOL_TYPE_MOUSE);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    /**
     * 发送返回键事件，并具备屏幕唤醒功能。
     *
     * <p><b>功能说明：</b>
     * <ul>
     *   <li>当屏幕亮屏时：模拟按下/释放返回键操作</li>
     *   <li>当屏幕熄屏时：自动唤醒屏幕（等效于按下电源键）</li>
     * </ul>
     *
     * <p><b>事件序列：</b>
     * <ul>
     *   <li>完整的按键操作需要先发送{@link Action#DOWN}，再发送{@link Action#UP}</li>
     *   <li>推荐使用无参版本自动完成完整按键序列</li>
     * </ul>
     *
     * <p><b>使用示例：</b>
     * <pre>{@code
     * // 方式1：手动控制按键序列
     * control.backOrTurnScreenOn(Action.DOWN);  // 按下返回键/唤醒屏幕
     * control.backOrTurnScreenOn(Action.UP);    // 释放按键
     *
     * // 方式2：自动完成完整按键操作（推荐）
     * control.backOrTurnScreenOn();
     * }</pre>
     *
     * @param action 按键动作类型（不能为null）
     * @throws ControlException     如果发生I/O错误
     * @throws NullPointerException 如果action参数为null
     * @see #backOrTurnScreenOn() 自动完成完整按键操作的便捷方法
     */
    public synchronized void backOrTurnScreenOn(Action action) {
        try {
            out.write(ControlType.TYPE_BACK_OR_SCREEN_ON);
            out.write(action.getCode());
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    /**
     * 自动发送完整的返回键按下和释放序列。
     *
     * <p>此方法是{@link #backOrTurnScreenOn(Action)}的便捷封装，
     * 会自动发送DOWN和UP两个动作，完成一次完整的按键操作。</p>
     */
    public void backOrTurnScreenOn() {
        backOrTurnScreenOn(Action.DOWN);
        backOrTurnScreenOn(Action.UP);
    }

    /**
     * 展开Android设备的通知栏。
     *
     * <p><b>功能说明：</b>
     * <ul>
     *   <li>会显示所有未读通知和系统状态信息</li>
     *   <li>在锁屏状态下可能受限（取决于设备策略）</li>
     * </ul>
     *
     * <p><b>典型使用场景：</b>
     * <ul>
     *   <li>检查未读通知</li>
     *   <li>查看系统状态信息（如网络、电量等）</li>
     * </ul>
     *
     * @throws ControlException 如果发生I/O错误
     * @see #collapsePanels() 收起通知栏
     * @see #expandSettingsPanel() 展开快捷设置面板
     */
    public synchronized void expandNotificationPanel() {
        try {
            out.write(ControlType.TYPE_EXPAND_NOTIFICATION_PANEL);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    /**
     * 展开Android设备的快捷设置面板。
     *
     * <p><b>功能说明：</b>
     * <ul>
     *   <li>会显示快速设置开关（如Wi-Fi、蓝牙等）和亮度调节</li>
     *   <li>在Android不同版本中表现可能有所差异</li>
     * </ul>
     *
     * <p><b>与通知栏的区别：</b>
     * <ul>
     *   <li>通知栏主要显示通知内容</li>
     *   <li>快捷设置面板主要提供系统快捷开关</li>
     * </ul>
     *
     * @throws ControlException 如果发生I/O错误
     * @see #expandNotificationPanel() 展开通知栏
     * @see #collapsePanels() 收起所有面板
     */
    public synchronized void expandSettingsPanel() {
        try {
            out.write(ControlType.TYPE_EXPAND_SETTINGS_PANEL);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    /**
     * 收起所有展开的系统面板（通知栏/快捷设置面板）。
     *
     * @throws ControlException 如果发生I/O错误
     * @see #expandNotificationPanel() 展开通知栏
     * @see #expandSettingsPanel() 展开快捷设置面板
     */
    public synchronized void collapsePanels() {
        try {
            out.write(ControlType.TYPE_COLLAPSE_PANELS);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }


    /**
     * 获取Android设备剪贴板中的文本内容。
     *
     * <p><b>功能说明：</b>
     * <ul>
     *   <li>读取设备当前剪贴板内容（仅支持文本格式）</li>
     *   <li>自动清空输入缓冲区，确保获取最新数据</li>
     *   <li>内容以UTF-8编码格式传输</li>
     * </ul>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>需要设备授予剪贴板访问权限</li>
     *   <li>只能获取文本内容，非文本内容将返回空字符串</li>
     *   <li>在多应用共享剪贴板环境下可能读取到其他应用的内容</li>
     * </ul>
     *
     * @param copyKey true = 拷贝, false = 剪切
     * @return 剪贴板中的文本内容
     * @throws ControlException 如果发生I/O错误或权限不足
     * @see #setClipboard(String, boolean) 设置剪贴板内容
     */
    public synchronized String getClipboard(boolean copyKey) {
        try {
            int available;
            while ((available = in.available()) > 0) {
                // 每次最多读1024字节，或者也可以直接读available个字节
                byte[] buffer = new byte[Math.min(available, 1024)];
                // 这里可以忽略读取到的数据，因为我们只是要清空
                in.read(buffer);
            }
            out.write(ControlType.TYPE_GET_CLIPBOARD);
            out.write(copyKey ? 1 : 2);
            out.flush();
            int read = in.readUnsignedByte();
            assert read == 0;
            int length = in.readInt();
            byte[] bytes = in.readNBytes(length);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }


    /**
     * 设置Android设备剪贴板内容并可选择立即粘贴。
     *
     * <p><b>功能说明：</b>
     * <ul>
     *   <li>设置设备剪贴板内容（仅支持文本格式）</li>
     *   <li>可选是否自动粘贴到当前输入框</li>
     *   <li>内容以UTF-8编码格式传输</li>
     * </ul>
     *
     * <p><b>参数说明：</b>
     * <ul>
     *   <li>当paste=true时：
     *     <ul>
     *       <li>需要当前有激活的输入框</li>
     *       <li>模拟系统粘贴操作</li>
     *       <li>效果等同于手动长按并选择粘贴</li>
     *     </ul>
     *   </li>
     *   <li>当paste=false时：
     *     <ul>
     *       <li>仅更新剪贴板内容</li>
     *       <li>不会影响当前UI状态</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param text  要设置的文本内容（不能为null）
     * @param paste 是否立即粘贴到当前输入框
     * @throws ControlException     如果发生I/O错误或权限不足
     * @throws NullPointerException 如果text参数为null
     * @see #getClipboard(boolean) 获取剪贴板内容
     */
    public synchronized void setClipboard(String text, boolean paste) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        try {
            out.write(ControlType.TYPE_SET_CLIPBOARD);
            out.writeLong(0);
            out.writeBoolean(paste);
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    /**
     * 旋转Android设备屏幕方向。
     *
     * @throws ControlException 如果发生I/O错误或操作不被允许
     */
    public synchronized void rotateDevice() {
        try {
            out.write(ControlType.TYPE_ROTATE_DEVICE);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    public synchronized void openHardKeyboardSettings() {
        try {
            out.write(ControlType.OPEN_HARD_KEYBOARD_SETTINGS);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    public synchronized void resetVideo() {
        try {
            out.write(ControlType.RESET_VIDEO);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }

    public synchronized void startApp(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        if (length > 0xFF) {
            throw new IllegalArgumentException();
        }
        try {
            out.write(ControlType.START_APP);
            out.write(length);
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            throw new ControlException(e);
        }
    }
}
