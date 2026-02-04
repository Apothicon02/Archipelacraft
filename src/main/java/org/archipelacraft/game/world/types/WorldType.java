package org.archipelacraft.game.world.types;

import org.archipelacraft.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.archipelacraft.game.world.World.*;

public class WorldType {
    public Path getWorldPath() {
        return Path.of("none");
    }

    public void generate() throws IOException {
        if (Files.exists(getWorldPath())) {
            loadWorld(getWorldPath()+"/");
        } else {
            createNew();
        }
    }

    public void createNew() {}
}
