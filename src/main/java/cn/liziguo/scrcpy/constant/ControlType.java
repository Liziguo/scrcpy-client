package cn.liziguo.scrcpy.constant;

/**
 * @author Liziguo
 * @date 2025-06-11
 */
public interface ControlType {
    byte TYPE_INJECT_KEYCODE = 0;
    byte TYPE_INJECT_TEXT = 1;
    byte TYPE_INJECT_TOUCH_EVENT = 2;
    byte TYPE_INJECT_SCROLL_EVENT = 3;
    byte TYPE_BACK_OR_SCREEN_ON = 4;
    byte TYPE_EXPAND_NOTIFICATION_PANEL = 5;
    byte TYPE_EXPAND_SETTINGS_PANEL = 6;
    byte TYPE_COLLAPSE_PANELS = 7;
    byte TYPE_GET_CLIPBOARD = 8;
    byte TYPE_SET_CLIPBOARD = 9;
    byte TYPE_SET_SCREEN_POWER_MODE = 10;
    byte TYPE_ROTATE_DEVICE = 11;
    byte UHID_CREATE = 12;
    byte UHID_INPUT = 13;
    byte UHID_DESTROY = 14;
    byte OPEN_HARD_KEYBOARD_SETTINGS = 15;
    byte START_APP = 16;
    byte RESET_VIDEO = 17;
}
