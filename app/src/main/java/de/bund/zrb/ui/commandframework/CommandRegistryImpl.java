package de.bund.zrb.ui.commandframework;

import com.google.gson.Gson;
import de.bund.zrb.dto.RecordedEvent;
import de.bund.zrb.service.RecorderService;

import java.util.*;

//ToDo: Change to singleton an pull members up into the interface
public class CommandRegistryImpl implements CommandRegistry {

    private static final Map<String, MenuCommand> registry = new LinkedHashMap<>();

    private static CommandRegistryImpl instance;

    private CommandRegistryImpl() {}

    public static CommandRegistryImpl getInstance() {
        if (instance == null) {
            synchronized (CommandRegistryImpl.class) {
                if (instance == null) {
                    instance = new CommandRegistryImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(MenuCommand menuCommand) {
        registry.put(menuCommand.getId(), menuCommand);
    }

    public Optional<MenuCommand> getById(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    public Collection<MenuCommand> getAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public void clear() {
        registry.clear();
    }
}
