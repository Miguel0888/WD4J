package wd4j.impl.websocket;

import wd4j.impl.markerInterfaces.ResultData;

/**
 * Represents a successful response to a command. It is NOT part of the WebDriver protocol. The protocol
 * defines only the CommandResponse and a separate ErrorResponse. Since an ErrorResponse is technically a
 * response to a command, it can be handled as a CommandResponse!
 * It is just an alternative: The correct way in Java is to use the Exception mechanism for errors, what is done too.
 *
 * @param <T>
 */
//public class CommandResponseImpl<T extends ResultData> implements CommandResponse<T> {
public class CommandResponseImpl<T /*extends ResultData*/> implements CommandResponse<T> {
    private String type = "success";
    private int id;
    private T result;

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public T getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "SuccessResponse{" +
                "type='" + type + '\'' +
                ", id=" + id +
                ", result=" + result +
                '}';
    }
}
