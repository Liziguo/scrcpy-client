package cn.liziguo.scrcpy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Liziguo
 * @date 2025-06-10
 */
public class CommandUtil {

    public static String cmd(String... commands) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true); // 合并错误流

            Process process = pb.start();

            // 读取命令输出
            try (InputStream inputStream = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            process.waitFor();
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void cmdIgnore(String... commands) {
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> cmdLines(String... commands) {
        List<String> list = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取命令输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    list.add(line);
                }
            }
            process.waitFor();
            return list;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> abdDevices() {
        List<String> list = new ArrayList<>();
        List<String> lines = CommandUtil.cmdLines(ScrcpyClient.ADB_PATH, "devices");
        for (String line : lines.subList(1, lines.size())) {
            String[] split = line.split("\t", -1);
            if (split.length >= 2 && split[1].equalsIgnoreCase("device")) {
                list.add(split[0]);
            }
        }
        return list;
    }
}
