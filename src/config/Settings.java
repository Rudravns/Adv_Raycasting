package config;

public class Settings {
    
    public static int screenWidth = 1000;
    public static int screenHeight = 800;

    public static int mapWidth = 20;
    public static int mapHeight = 20;

    public static int tileSize2d = 50; // Size of each tile in pixels
    public static int tileSizeMiniMap = 10; // Size of each tile in pixels for minimap

    public static boolean is2DMode = false; // Flag to indicate if the game is in 2D mode
    public static boolean isMiniMap = true; // Flag to indicate if the minimap is enabled
    public static boolean simpleWallColors = false; // Use shaded solid colors instead of wall textures


    public static final float PLAYER_SPEED = 4.0f; // Maximum player speed in map tiles per second
    public static final float ROTATION_SPEED = 5.0f; // Player rotation speed
    public static final float PLAYER_ACCELERATION = 18.0f; // Acceleration in map tiles per second squared
    public static final float PLAYER_DRAG = 6.0f; // Velocity damping per second
    public static final float PLAYER_RADIUS = 0.2f; // Collision-circle radius in map tiles


    public static boolean mouseLocked = true; // Flag to indicate if the mouse is locked to the center of the screen
    public static float MOUSE_SENSITIVITY = 0.5f; // Mouse sensitivity for looking around
    public static float fov = 90.0f; // Field of view in degrees
    public static int numRays = 500; // Number of rays to cast for rendering
    public static float maxRenderDistance = 20.0f; // Maximum distance to render walls
    public static float wallHeight = 20.0f; // Height of walls in map tiles
    public static float PLAYER_LIGHT_RADIUS = 8.0f;
    public static float PLAYER_LIGHT_INTENSITY = 1.0f;
    public static float PLAYER_LIGHT_ATTENUATION = 2.0f;
    
}