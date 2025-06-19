package de.bund.zrb.impl.webdriver.command.request.parameters.input.sourceActions;

/**
 * Repräsentiert eine "pause"-Aktion für Tasten, Pointer, Wheel-Input und None-Input.
 */
class PauseAction implements KeySourceAction, PointerSourceAction, WheelSourceAction, NoneSourceAction {
    private final String type = "pause";
    private final Integer duration; // Optional

    public PauseAction(Integer duration) {
        this.duration = duration;
    }

    @Override
    public String getType() {
        return type;
    }

    public Integer getDuration() {
        return duration;
    }
}