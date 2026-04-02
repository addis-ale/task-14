package com.exam.system.notification;

public final class DeliveryStatus {

    public static final String PENDING = "PENDING";
    public static final String SENDING = "SENDING";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";
    public static final String HELD_DND = "HELD_DND";

    private DeliveryStatus() {
    }
}
