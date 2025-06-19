package de.bund.zrb.command.request.parameters.input.sourceActions;

import com.google.gson.Gson;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import de.bund.zrb.impl.webdriver.mapping.GsonMapperFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourceActionsTest {

    private final Gson gson = GsonMapperFactory.getGson();

    @Test
    void testNoneSourceActionsSerialization() {
        SourceActions.NoneSourceActions original = new SourceActions.NoneSourceActions("42", Arrays.asList(new PauseAction(150)));

        String json = gson.toJson(original);
        SourceActions.NoneSourceActions deserialized = gson.fromJson(json, SourceActions.NoneSourceActions.class);

        assertEquals(original.getType(), deserialized.getType());
        assertEquals(((PauseAction) original.getActions().get(0)).getType(), "pause");
        assertEquals(((PauseAction) original.getActions().get(0)).getDuration(), 150);
        assertEquals(original.getActions().size(), deserialized.getActions().size());
    }

    @Test
    void testKeySourceActionsSerialization() {
        SourceActions.KeySourceActions original = new SourceActions.KeySourceActions("key1",
                Arrays.asList(new KeySourceAction.KeyDownAction("A"), new KeySourceAction.KeyUpAction("A")));

        String json = gson.toJson(original);
        SourceActions.KeySourceActions deserialized = gson.fromJson(json, SourceActions.KeySourceActions.class);

        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getActions().size(), deserialized.getActions().size());
        assertEquals(original.getActions().get(0).getType(), deserialized.getActions().get(0).getType());
    }

    @Test
    void testPointerSourceActionsSerialization() {
        SourceActions.PointerSourceActions original = new SourceActions.PointerSourceActions(
                "pointer1",
                new SourceActions.PointerSourceActions.PointerParameters(
                        SourceActions.PointerSourceActions.PointerParameters.PointerType.PEN),
                Arrays.asList(
                        new PointerSourceAction.PointerMoveAction(50, 75, 200L, Origin.FixedOrigin.POINTER, new PointerSourceAction.PointerCommonProperties()),
                        new PointerSourceAction.PointerDownAction(1, new PointerSourceAction.PointerCommonProperties())
                )
        );

        String json = gson.toJson(original);
        SourceActions.PointerSourceActions deserialized = gson.fromJson(json, SourceActions.PointerSourceActions.class);

        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getParameters().getType(), deserialized.getParameters().getType());
        assertEquals(original.getActions().size(), deserialized.getActions().size());
        assertEquals(original.getActions().get(0).getType(), deserialized.getActions().get(0).getType());
    }

    @Test
    void testWheelSourceActionsSerialization() {
        SourceActions.WheelSourceActions original = new SourceActions.WheelSourceActions("wheel1", Arrays.asList(
                new WheelSourceAction.WheelScrollAction(100, 200, 10, 20, 300L, Origin.FixedOrigin.VIEWPORT)
        ));

        String json = gson.toJson(original);
        SourceActions.WheelSourceActions deserialized = gson.fromJson(json, SourceActions.WheelSourceActions.class);

        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getActions().size(), deserialized.getActions().size());
        assertEquals(original.getActions().get(0).getType(), deserialized.getActions().get(0).getType());
        assertEquals(((WheelSourceAction.WheelScrollAction)original.getActions().get(0)).getX(), ((WheelSourceAction.WheelScrollAction)deserialized.getActions().get(0)).getX());
    }

    @Test
    void testSourceActionsPolymorphism() {
        List<SourceActions> actions = Arrays.asList(
                new SourceActions.KeySourceActions("key1", Arrays.asList(new KeySourceAction.KeyDownAction("B"))),
                new SourceActions.PointerSourceActions("pointer1",
                        new SourceActions.PointerSourceActions.PointerParameters(
                                SourceActions.PointerSourceActions.PointerParameters.PointerType.TOUCH),
                        Arrays.asList(new PointerSourceAction.PointerMoveAction(20, 30, 150L, Origin.FixedOrigin.VIEWPORT,
                                new PointerSourceAction.PointerCommonProperties()))
                ),
                new SourceActions.WheelSourceActions("wheel1", Arrays.asList(
                        new WheelSourceAction.WheelScrollAction(5, 5, 2, 2, 250L, Origin.FixedOrigin.POINTER)
                ))
        );

        String json = gson.toJson(actions);
        SourceActions[] deserialized = gson.fromJson(json, SourceActions[].class);

        assertEquals(actions.size(), deserialized.length);
        for (int i = 0; i < actions.size(); i++) {
            System.out.println("Original Type: " + actions.get(i).getClass().getSimpleName());
            System.out.println("Deserialized Type: " + deserialized[i].getClass().getSimpleName());
            assertEquals(actions.get(i).getType(), deserialized[i].getType());
        }
    }

}
