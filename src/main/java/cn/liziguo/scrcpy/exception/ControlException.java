package cn.liziguo.scrcpy.exception;

/**
 * @author Liziguo
 * @date 2025-06-12
 */
public class ControlException extends RuntimeException {
    public ControlException() {
    }

    public ControlException(String message) {
        super(message);
    }

    public ControlException(String message, Throwable cause) {
        super(message, cause);
    }

    public ControlException(Throwable cause) {
        super(cause);
    }

    public ControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
