package org.archipelacraft.game.world.types;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.archipelacraft.game.world.World.*;

public class WorldType {
    public Path getWorldPath() {
        return Path.of("none");
    }

    public void generate() throws IOException {
        generated = false;
        if (Files.exists(getWorldPath())) {
            loadWorld(getWorldPath()+"/");
        } else {
            createNew();
        }
        generated = true;
    }

    public void createNew() {}
}
