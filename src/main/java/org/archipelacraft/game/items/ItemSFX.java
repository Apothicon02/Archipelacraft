package org.archipelacraft.game.items;

import org.archipelacraft.game.audio.AudioController;
import org.archipelacraft.game.audio.SFX;
import org.archipelacraft.game.audio.Source;
import org.joml.Vector3f;

public class ItemSFX {
    public final SFX[] placeIds;
    public final float placeGain;
    public final float placePitch;

    public ItemSFX(SFX[] placeIds, float placeGain, float placePitch) {
        this.placeIds = placeIds;
        this.placeGain = placeGain;
        this.placePitch = placePitch;
    }

    public void placed(Vector3f pos) {
        Source placeSource = new Source(new Vector3f(pos.x, pos.y, pos.z), (float) (placeGain+((placeGain*Math.random())/3)), (float) (placePitch+((placePitch*Math.random())/3)), 0, 0);
        AudioController.disposableSources.add(placeSource);
        placeSource.play(placeIds[(int) (Math.random()*placeIds.length)]);
    }
}
