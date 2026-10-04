
import java.util.ArrayList;

import static com.raylib.Raylib.*;
import static com.raylib.Raylib.KeyboardKey.*;
import com.raylib.*;

import Casting.Ray;
import Casting.Wall;
import Entities.Player;
import config.Settings;
import lighting.FloorCeilingShader;
import lighting.LightingManager;
import lighting.PointLight;

public class Main {

    // 1. Declare width and height as static class variables so main() can access them

    public static Map map = new Map(20, 20);
    public static float dt = 0.0f; // Delta time (time between frames)
    public static Player player;
    public static ArrayList<Ray> rays = new ArrayList<>();
    public static Wall wall = new Wall();
    private static final FloorCeilingShader floorCeilingShader = new FloorCeilingShader();
    private static PointLight playerLight;
    private static final PointLight environmentLight = new PointLight(4.5f, 7.5f, 5.0f, GREEN);
    private static final PointLight environmentLight2 = new PointLight(5f, 7.5f, 5.0f, RED, 5.0f);

    static { //pre setup before main is called
        map.setupMap(Map.Starter_maps.MAP1);
        // Use the display dimensions for a fullscreen-sized window.
        Settings.screenWidth = getScreenWidth();
        Settings.screenHeight = getScreenHeight();

        // Initialize player at the spawn point noted by 3
        Vector2 spawnPoint = map.getPlayerSpawnPoint();
        if (spawnPoint != null) {
            player = new Player(spawnPoint, 0.0f);
        }
        else {
            System.err.println("No spawn point found in the map!");
            System.exit(1);
        }

    }

    public static void main(String[] args) {
        initWindow(Settings.screenWidth, Settings.screenHeight, "Raylib + Java");
        wall.loadTexture();
        floorCeilingShader.load();
        playerLight = new PointLight(
                player.getCenterPosition().x(),
                player.getCenterPosition().y(),
                Settings.PLAYER_LIGHT_RADIUS,
                RAYWHITE,
                Settings.PLAYER_LIGHT_INTENSITY,
                Settings.PLAYER_LIGHT_ATTENUATION,
                player.getAngle());
        LightingManager.addLight(playerLight);
        LightingManager.addLight(environmentLight);
        LightingManager.addLight(environmentLight2);
        if (Settings.mouseLocked) {
           disableCursor();
        }
        setTargetFPS(60);

        while (!windowShouldClose()) {
            // 2. Update your static variables with the current screen size if the window resizes
            Settings.screenWidth = getScreenWidth();
            Settings.screenHeight = getScreenHeight();
            dt = com.raylib.Raylib.getFrameTime(); // Update delta time
            
            // events
            if (isKeyPressed(KEY_M)) {
                Settings.isMiniMap = !Settings.isMiniMap;
            }
            if (isKeyPressed(KEY_P)) {
                Settings.mouseLocked = !Settings.mouseLocked;
                if (Settings.mouseLocked) {
                    disableCursor();
                } else {
                    enableCursor();
                }
            }
            if (isKeyPressed(KEY_G)) {
                Settings.is2DMode = !Settings.is2DMode;
            }
            if (isKeyPressed(KEY_T)) {
                Settings.simpleWallColors = !Settings.simpleWallColors;
            }

            if (isKeyPressed(KEY_I)){
                playerLight.intensity = (playerLight.intensity == 0.0f) ? Settings.PLAYER_LIGHT_INTENSITY : 0.0f;
            }

            beginDrawing();
            clearBackground(RAYWHITE);

            // Update first, then draw
            player.updatePosition(dt, map::getTile, Map.TileType.WALL.getValue());
            LightingManager.updateLightDirection(playerLight, player.getAngle());
            LightingManager.updateLightPosition(
                    playerLight, player.getCenterPosition().x(), player.getCenterPosition().y());
            updateRays();

            if (!Settings.is2DMode) {
                floorCeilingShader.draw(
                        player.getCenterPosition(), player.getAngle(), playerLight, environmentLight, environmentLight2);
            }

            //draw stuff here (Order matters)
            wall.draw(player.getCenterPosition(), playerLight, environmentLight, environmentLight2); // walls

            drawMap();// map/mini-map

            player.draw(map.getWidth(), map.getHeight()); //player
            
            
            // rays
            for (Ray ray : rays) {
                ray.draw(map.getWidth(), map.getHeight());
            }

            //text
            drawFPS(20, 20);
            //draw pos
            String posText = String.format("Player Position: (%.2f, %.2f)", player.getX(), player.getY());
            drawText(posText, 20, 50, 20, BLACK);
            String angleText = String.format("Player Angle: %.2f radians", player.getAngle());
            drawText(angleText, 20, 80, 20, BLACK);
            // Crosshair dot at screen center
            if (!Settings.is2DMode) {
                int cx = Settings.screenWidth / 2;
                int cy = Settings.screenHeight / 2;
                drawCircle(cx, cy, 3, WHITE);
                drawCircle(cx, cy, 2, DARKGRAY);
            }
            endDrawing();
        }

        wall.unloadTexture();
        floorCeilingShader.unload();
        closeWindow();
    }
    
    public static void updateRays() {
        rays.clear();
        wall.clearWalls();
        float startAngle = player.getAngle() - (float) Math.toRadians(Settings.fov) / 2.0f;
        float angleStep = (float) Math.toRadians(Settings.fov) / Settings.numRays;

        for (int i = 0; i < Settings.numRays; i++) {
            float rayAngle = startAngle + i * angleStep;
            Ray ray = new Ray(player.getCenterPosition(), rayAngle);
            ray.cast(map::getTile, Map.TileType.WALL.getValue(), Settings.maxRenderDistance);

            // Fish-eye correction: use the perpendicular distance (not Euclidean) so walls
            // appear straight rather than curved (barrel-distorted).
            float angleOffset  = rayAngle - player.getAngle();
            float perpDistance = ray.Distance() * (float) Math.cos(angleOffset);

            wall.addWall(
                    perpDistance,
                    ray.hitVerticalSide(),
                    ray.getTextureOffset(),
                    ray.getHitPosition().x(),
                    ray.getHitPosition().y(),
                    ray.hitVerticalSide() ? (ray.getDirX() > 0.0f ? -1.0f : 1.0f) : 0.0f,
                    ray.hitVerticalSide() ? 0.0f : (ray.getDirY() > 0.0f ? -1.0f : 1.0f)
            );

            rays.add(ray);
        }
    }

    public static void drawMap() {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                int tileType = map.getTile(x, y);
                if (tileType == Map.TileType.WALL.getValue()) {
                    if (!Settings.is2DMode && Settings.isMiniMap) {
                        int posX = (x * Settings.tileSizeMiniMap) + (Settings.screenWidth - (map.getWidth() * Settings.tileSizeMiniMap)); //top right corner of the map
                        int posY = y * Settings.tileSizeMiniMap;
                        com.raylib.Raylib.drawRectangle(posX, posY, Settings.tileSizeMiniMap, Settings.tileSizeMiniMap, BLACK);
                    }
                    else if (Settings.is2DMode) {
                        int posX = (x * Settings.tileSize2d) + ((Settings.screenWidth - (map.getWidth() * Settings.tileSize2d)) / 2); //center the map
                        int posY = (y * Settings.tileSize2d) + ((Settings.screenHeight - (map.getHeight() * Settings.tileSize2d)) / 2); //center the map
                        com.raylib.Raylib.drawRectangle(posX, posY, Settings.tileSize2d, Settings.tileSize2d, BLACK);
                    }
                }
            }
        }
    }
}
