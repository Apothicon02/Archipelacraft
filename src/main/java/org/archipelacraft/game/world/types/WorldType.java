package org.archipelacraft.game.world.types;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.archipelacraft.game.world.World.*;

public class WorldType {
    public Random rand() {
        return null;
    }
    public Path getWorldPath() {
        return Path.of("none");
    }
    public String getWorldTypeName() {
        return "none";
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
