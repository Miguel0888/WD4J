package wd4j.impl.module.command;

import wd4j.core.CommandImpl;
import wd4j.impl.module.generic.Command;

import java.util.List;

public class Input {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class PerformActions extends CommandImpl<PerformActions.ParamsImpl> {

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

    public static class ReleaseActions extends CommandImpl<ReleaseActions.ParamsImpl> {

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

    public static class SetFiles extends CommandImpl<SetFiles.ParamsImpl> {

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