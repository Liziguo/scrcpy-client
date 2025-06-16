# Scrcpy Client - Android Screen Mirroring and Control Tool Without Root

[English](./README.md) | [简体中文](./README_zh.md)

A root-free real-time display and control tool for Android devices, supporting multi-touch and providing flexible
secondary development interfaces, enabling easy remote control and screen mirroring of Android devices.

## 🖥️ Demo

![Demo Image](images/Demo1.gif)
![Demo Image](images/Demo2.gif)

## ✨ Core Features

- **Real-time HD Screen Mirroring** - Low latency, high-quality screen mirroring
- **Bidirectional Control** - Supports touch and keyboard input for device control
- **Multi-touch Support** - Full multi-touch gesture support
- **Developer-Friendly** - Clean API interfaces for easy secondary development
- **Efficient Transmission** - Optimized video encoding and network transmission
- **No Root Required** - Works without device root permissions

## ⚙️ Requirements

### Development Environment

- Java 21+
- Windows 10/11 x64 system
- Maven build tool

### Android Device

- Minimum API 21 (Android 5.0), no root required
- Ensure you have <a href="https://developer.android.com/tools/adb?hl=zh-cn#Enabling">enabled USB debugging</a> on your
  device
- On some devices, you may also need to enable an additional option "USB debugging (Security settings)" which is
  different from regular USB debugging. This software requires this setting to control your device.

> **Tip**: If wireless debugging fails to connect, try connecting via USB first - this often automatically restores
> wireless connectivity.

![USB Debugging (Security Settings).png](images/USB%20Debugging%20%28Security%20Settings%29.png)

## 🚀 Quick Start

### 1. Clone the project:

```shell
git clone https://github.com/Liziguo/scrcpy-client
cd scrcpy-client
```

### 2. Import to IDE:

- IntelliJ IDEA/Eclipse: Import as Maven project
- Wait for dependencies to resolve automatically

### 3. Run Example

Open test file:

`src/test/java/cn/liziguo/scrcpy/ScrcpyTest.java`

Execute main method and follow prompts to connect device

## ⌨️ Control Shortcuts

| Key     | Function                                   |
|---------|--------------------------------------------|
| Esc     | Back button (can wake screen when off)     |
| F1      | Press Home button                          |
| F2      | Turn off screen (while keeping mirroring)  |
| F3      | Turn on screen                             |
| F4      | Open notification panel                    |
| F5      | Open quick settings panel                  |
| F6      | Collapse notification/settings panel       |
| F7      | Read device clipboard                      |
| F8      | Write to device clipboard                  |
| F9      | Rotate device screen                       |
| F10     | Text input (requires active input field)   |
| F11     | Open the hardware keyboard settings        |
| F12     | Open WeChat `com.tencent.mm`               |
| W/A/S/D | Multi-touch test (triggers in center area) |

## 💻 Development Example

```java
import cn.liziguo.scrcpy.ScrcpyClient;

public class Main {
    public static void main(String[] args) {
        // Create client instance
      ScrcpyClient scrcpyClient = new ScrcpyClient();

        // Configure connection parameters
      scrcpyClient.setDevice("your-android-device");      // Get from adb devices
      // scrcpyClient.setDevice("192.168.1.123:5555");    // Optional wireless connection (if wireless debugging fails, try USB connection first)
      scrcpyClient.setMaxWidth(1080);                     // Max width
      scrcpyClient.setMaxFps(60);                         // Max FPS
      scrcpyClient.setBitrate(2_000_000);                 // Bitrate (2Mbps)

        // Register frame callback (optional)
      scrcpyClient.setOnFrame(frame -> {
            // Process video frames
        });

        // Start connection
      scrcpyClient.start();
    }
}
```

### Complete Feature Demo

Check `src/test/java/cn/liziguo/scrcpy/ScrcpyTest.java` for complete demo code.

## 🙏 Acknowledgments

This project is built upon these excellent works:

- Core Engine: [scrcpy](https://github.com/Genymobile/scrcpy) (Genymobile)
- Project Inspiration: [py-scrcpy-client](https://github.com/leng-yue/py-scrcpy-client) (leng-yue)
