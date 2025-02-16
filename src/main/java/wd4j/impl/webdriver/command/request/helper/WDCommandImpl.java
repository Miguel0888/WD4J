package wd4j.impl.webdriver.command.request.helper;

import wd4j.impl.websocket.WDCommand;

public class WDCommandImpl<T extends WDCommand.Params> implements WDCommand {
    private Integer id;
    private final String method;
    protected T params;

    public WDCommandImpl(String method, T params) {
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
