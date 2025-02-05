package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.websocket.Command;

import java.util.List;

public class Input {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class PerformActions extends CommandImpl<PerformActions.ParamsImpl> implements CommandData {

        public PerformActions(String contextId, List<Action> actions) {
            super("input.performActions", new ParamsImpl(contextId, actions));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final List<Action> actions;

            public ParamsImpl(String contextId, List<Action> actions) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (actions == null || actions.isEmpty()) {
                    throw new IllegalArgumentException("Actions list must not be null or empty.");
                }
                this.context = contextId;
                this.actions = actions;
            }
        }

        public static class Action {
            private final String type;
            private final String id;

            public Action(String type, String id) {
                this.type = type;
                this.id = id;
            }
        }
    }

    public static class ReleaseActions extends CommandImpl<ReleaseActions.ParamsImpl> implements CommandData {

        public ReleaseActions(String contextId) {
            super("input.releaseActions", new ParamsImpl(contextId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;

            public ParamsImpl(String contextId) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
            }
        }
    }

    public static class SetFiles extends CommandImpl<SetFiles.ParamsImpl> implements CommandData {

        public SetFiles(String elementId, List<String> filePaths) {
            super("input.setFiles", new ParamsImpl(elementId, filePaths));
        }

        public static class ParamsImpl implements Command.Params {
            private final String element;
            private final List<String> files;

            public ParamsImpl(String elementId, List<String> filePaths) {
                if (elementId == null || elementId.isEmpty()) {
                    throw new IllegalArgumentException("Element ID must not be null or empty.");
                }
                if (filePaths == null || filePaths.isEmpty()) {
                    throw new IllegalArgumentException("File paths list must not be null or empty.");
                }
                this.element = elementId;
                this.files = filePaths;
            }
        }
    }

}