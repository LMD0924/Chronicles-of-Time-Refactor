package com.chroniclesoftime.shared;

import java.util.UUID;

public final class Ids {
    private Ids() {}
    public static String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
    public static String token() { return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""); }
}
