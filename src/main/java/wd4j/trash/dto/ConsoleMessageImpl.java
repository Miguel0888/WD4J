package wd4j.trash.dto;

import wd4j.api.ConsoleMessage;
import wd4j.api.JSHandle;
import wd4j.api.Page;

import java.util.List;

public class ConsoleMessageImpl implements ConsoleMessage {
    private final String text;
    private final String type;
    private final String location;
    private final List<JSHandle> args;
    private final Page page;

    public ConsoleMessageImpl(String text, String type, String location, List<JSHandle> args, Page page) {
        this.text = text;
        this.type = type;
        this.location = location;
        this.args = args;
        this.page = page;
    }

    @Override
    public List<JSHandle> args() {
        return args;
    }

    @Override
    public String location() {
        return location;
    }

    @Override
    public Page page() {
        return page;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public String type() {
        return type;
    }
}

