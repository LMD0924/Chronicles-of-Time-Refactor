package com.chroniclesoftime.shared;

/** 全站统一响应包装。 */
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public Result() {}
    public Result(int code, String message, T data) { this.code = code; this.message = message; this.data = data; }

    public static <T> Result<T> ok(T data) { return new Result<>(200, "ok", data); }
    public static <T> Result<T> ok() { return new Result<>(200, "ok", null); }
    public static <T> Result<T> fail(int code, String message) { return new Result<>(code, message, null); }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
