package Casting;
import java.util.ArrayList;

import static com.raylib.Raylib.DARKGRAY;
import static com.raylib.Raylib.drawRectangle;

import config.Settings;


public class Wall {
    private ArrayList<Float> walls = new ArrayList<>();
    private float Wall_width;
    public Wall() {
        Wall_width = (Settings.screenWidth*2) / Settings.numRays;
    }

    public void addWall(float rayLength) {
        float height = (Settings.screenHeight / rayLength) + Settings.wallHeight;
        walls.add(height);
    }

    public void clearWalls() {
        walls.clear();
    }

    public void draw() {
        for (int i = 0; i < walls.size(); i++) {
            float height = walls.get(i);
            float x = i * Wall_width;
            float y = (Settings.screenHeight - height) / 2.0f; // Center the wall vertically
            drawRectangle((int)x, (int)y, (int)Wall_width, (int)height, DARKGRAY);
        }
    }

}