package com.exam.system.service.impl.delivery;

public class DeliveryAttemptResult {

    private final boolean success;
    private final boolean channelUnavailable;
    private final String message;

    private DeliveryAttemptResult(boolean success, boolean channelUnavailable, String message) {
        this.success = success;
        this.channelUnavailable = channelUnavailable;
        this.message = message;
    }

    public static DeliveryAttemptResult success() {
        return new DeliveryAttemptResult(true, false, "ok");
    }

    public static DeliveryAttemptResult unavailable(String message) {
        return new DeliveryAttemptResult(false, true, message);
    }

    public static DeliveryAttemptResult failed(String message) {
        return new DeliveryAttemptResult(false, false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isChannelUnavailable() {
        return channelUnavailable;
    }

    public String getMessage() {
        return message;
    }
}
