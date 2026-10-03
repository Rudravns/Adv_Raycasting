package Casting;

import static com.raylib.Raylib.*;
import com.raylib.Color;
import com.raylib.Rectangle;
import com.raylib.Texture;
import com.raylib.Vector2;
import config.Settings;
import lighting.LightingManager; // Import your new package here!

public class Wall {

    private final java.util.ArrayList<WallSlice> walls = new java.util.ArrayList<>();
    private Texture texture;

    // We can use base solid colors now since the lighting manager handles side dimming automatically!
    private final Color verticalTint = LIGHTGRAY;
    private final Color horizontalTint = RAYWHITE;

    public void loadTexture() {
        if (texture != null) {
            return;
        }
        texture = com.raylib.Raylib.loadTexture("Assets/textures/brickWall.jpg");
        if (texture.width() <= 0 || texture.height() <= 0) {
            texture = null;
            throw new IllegalStateException("Could not load wall texture: Assets/textures/brickWall.jpg");
        }
    }

    public void unloadTexture() {
        if (texture != null) {
            com.raylib.Raylib.unloadTexture(texture);
            texture = null;
        }
    }

    // UPDATE: Accept hitX and hitY world positions directly from your DDA raycaster
    public void addWall(float rayLength, boolean isVertical, float textureOffset, float hitX, float hitY) {
        float safeDistance = Math.max(rayLength, 0.0001f);
        float height = (Settings.screenHeight / safeDistance) + Settings.wallHeight;
        walls.add(new WallSlice(height, isVertical, textureOffset, hitX, hitY));
    }

    public void clearWalls() {
        walls.clear();
    }

    public void draw() {
        if (texture == null) {
            throw new IllegalStateException("Wall texture must be loaded before drawing walls");
        }
        float columnWidth = (float) Settings.screenWidth / Settings.numRays;
        for (int i = 0; i < walls.size(); i++) {
            WallSlice wall = walls.get(i);
            float x = i * columnWidth;
            float y = (Settings.screenHeight - wall.height) / 2.0f;
            float textureX = Math.min(texture.width() - 1,
                    (float) Math.floor(wall.textureOffset * texture.width()));
            Rectangle source = new Rectangle(textureX, 0.0f, 1.0f, texture.height());
            Rectangle destination = new Rectangle(x, y, columnWidth + 1.0f, wall.height);

            // UPDATE: Swap out old shader for the dynamic vector calculation
            Color tint = LightingManager.calculateLighting(
                    wall.isVertical ? verticalTint : horizontalTint,
                    wall.hitX,
                    wall.hitY,
                    wall.isVertical);

            if (Settings.simpleWallColors) {
                drawRectangle((int) x, (int) y, (int) Math.ceil(columnWidth + 1.0f),
                        (int) Math.ceil(wall.height), tint);
            } else {
                drawTexturePro(texture, source, destination, new Vector2(0.0f, 0.0f), 0.0f, tint);
            }
        }
    }

    private static final class WallSlice {

        private final float height;
        private final boolean isVertical;
        private final float textureOffset;
        // Added world variables
        private final float hitX;
        private final float hitY;

        private WallSlice(float height, boolean isVertical, float textureOffset, float hitX, float hitY) {
            this.height = height;
            this.isVertical = isVertical;
            this.textureOffset = textureOffset;
            this.hitX = hitX;
            this.hitY = hitY;
        }
    }
}
