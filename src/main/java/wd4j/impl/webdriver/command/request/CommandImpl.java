package wd4j.impl.webdriver.command.request;

import wd4j.impl.websocket.Command;

public class CommandImpl<T extends Command.Params> implements Command {
    private Integer id;
    private final String method;
    protected T params;

    public CommandImpl(String method, T params) {
        this.method = method;
        this.params = params;
    }

    @Override
    public String getName() {
        return method;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
