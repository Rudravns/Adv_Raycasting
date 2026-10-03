package Entities;

import java.util.function.IntBinaryOperator;

import com.raylib.Raylib;
import static com.raylib.Raylib.BLUE;
import static com.raylib.Raylib.KeyboardKey.KEY_A;
import static com.raylib.Raylib.KeyboardKey.KEY_D;
import static com.raylib.Raylib.KeyboardKey.KEY_LEFT;
import static com.raylib.Raylib.KeyboardKey.KEY_RIGHT;
import static com.raylib.Raylib.KeyboardKey.KEY_S;
import static com.raylib.Raylib.KeyboardKey.KEY_W; // 1. Added static import for drawTriangle
import static com.raylib.Raylib.drawTriangle;
import static com.raylib.Raylib.isKeyDown;
import com.raylib.Vector2;

import config.Settings;

public class Player {

    private Vector2 position;
    private Vector2 velocity;
    private float angle;

    public Player(float x, float y, float angle) {
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(0.0f, 0.0f);
        this.angle = angle;
    }

    public Player(Vector2 position, float angle) {
        this(position.x(), position.y(), angle);
    }

    public void updatePosition(float dt, IntBinaryOperator getTile, int wallTileType) {
        float frameTime = Math.min(Math.max(dt, 0.0f), 0.1f);
        float moveX = 0.0f;
        float moveY = 0.0f;
        Vector2 mouseDelta = Raylib.getMouseDelta();
        angle += mouseDelta.x() * Settings.MOUSE_SENSITIVITY * frameTime;

        if (isKeyDown(KEY_W)) {
            moveX += (float) Math.cos(angle);
            moveY += (float) Math.sin(angle);
        }
        if (isKeyDown(KEY_S)) {
            moveX -= (float) Math.cos(angle);
            moveY -= (float) Math.sin(angle);
        }
        if (isKeyDown(KEY_A)) {
            moveX += (float) Math.cos(angle - Math.PI / 2.0);
            moveY += (float) Math.sin(angle - Math.PI / 2.0);
        }
        if (isKeyDown(KEY_D)) {
            moveX += (float) Math.cos(angle + Math.PI / 2.0);
            moveY += (float) Math.sin(angle + Math.PI / 2.0);
        }

        if (isKeyDown(KEY_LEFT)) {
            angle -= Settings.ROTATION_SPEED * frameTime;
        }
        if (isKeyDown(KEY_RIGHT)) {
            angle += Settings.ROTATION_SPEED * frameTime;
        }

        float inputLength = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        if (inputLength > 0.0f) {
            velocity.x(velocity.x() + moveX / inputLength * Settings.PLAYER_ACCELERATION * frameTime);
            velocity.y(velocity.y() + moveY / inputLength * Settings.PLAYER_ACCELERATION * frameTime);
        } else {
            float drag = (float) Math.exp(-Settings.PLAYER_DRAG * frameTime);
            velocity.x(velocity.x() * drag);
            velocity.y(velocity.y() * drag);
        }

        float speed = (float) Math.sqrt(velocity.x() * velocity.x() + velocity.y() * velocity.y());
        if (speed > Settings.PLAYER_SPEED) {
            float speedScale = Settings.PLAYER_SPEED / speed;
            velocity.x(velocity.x() * speedScale);
            velocity.y(velocity.y() * speedScale);
        }

        moveWithCollisions(velocity.x() * frameTime, velocity.y() * frameTime, getTile, wallTileType);
    }

    private void moveWithCollisions(float deltaX, float deltaY, IntBinaryOperator getTile, int wallTileType) {
        float maxStep = Math.max(Settings.PLAYER_RADIUS * 0.5f, 0.01f);
        int steps = Math.max(1, (int) Math.ceil(
                Math.max(Math.abs(deltaX), Math.abs(deltaY)) / maxStep));
        float stepX = deltaX / steps;
        float stepY = deltaY / steps;

        for (int i = 0; i < steps; i++) {
            float candidateX = position.x() + stepX;
            if (collides(candidateX, position.y(), getTile, wallTileType)) {
                velocity.x(0.0f);
            } else {
                position.x(candidateX);
            }

            float candidateY = position.y() + stepY;
            if (collides(position.x(), candidateY, getTile, wallTileType)) {
                velocity.y(0.0f);
            } else {
                position.y(candidateY);
            }
        }
    }

    private boolean collides(float playerX, float playerY, IntBinaryOperator getTile, int wallTileType) {
        float centerX = playerX + 0.5f;
        float centerY = playerY + 0.5f;
        float radius = Settings.PLAYER_RADIUS;
        int minTileX = (int) Math.floor(centerX - radius);
        int maxTileX = (int) Math.floor(centerX + radius);
        int minTileY = (int) Math.floor(centerY - radius);
        int maxTileY = (int) Math.floor(centerY + radius);

        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                if (getTile.applyAsInt(tileX, tileY) != wallTileType) {
                    continue;
                }

                float closestX = Math.max(tileX, Math.min(centerX, tileX + 1.0f));
                float closestY = Math.max(tileY, Math.min(centerY, tileY + 1.0f));
                float distanceX = centerX - closestX;
                float distanceY = centerY - closestY;
                if (distanceX * distanceX + distanceY * distanceY < radius * radius) {
                    return true;
                }
            }
        }
        return false;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getCenterPosition() {
        return new Vector2(position.x() + 0.5f, position.y() + 0.5f);
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public float getAngle() {
        return angle;
    }

    public void draw(int mapWidth, int mapHeight) {
        if (!Settings.is2DMode && !Settings.isMiniMap) {
            return;
        }
        boolean drawMiniMap = !Settings.is2DMode && Settings.isMiniMap;
        float tileSize = drawMiniMap ? Settings.tileSizeMiniMap : Settings.tileSize2d;
        float mapOffsetX = drawMiniMap
                ? Settings.screenWidth - mapWidth * tileSize
                : (Settings.screenWidth - mapWidth * tileSize) / 2.0f;
        float mapOffsetY = drawMiniMap
                ? 0.0f
                : (Settings.screenHeight - mapHeight * tileSize) / 2.0f;
        float centerX = mapOffsetX + (position.x() + 0.5f) * tileSize;
        float centerY = mapOffsetY + (position.y() + 0.5f) * tileSize;
        float radius = Math.max(6.0f, tileSize * 0.22f);

        //drawCircle(Math.round(centerX), Math.round(centerY), radius, RED);

        // --- DRAW DIRECTIONAL TRIANGLE ---
        float triangleSize = radius * 1.5f;

        // 2. FIXED: Removed 'position.getAngle()' which breaks build. Uses Player field 'this.angle' directly.
        float currentAngle = this.angle;

        // 3. Calculate the tip of the triangle (facing forward)
        float tipX = centerX + (float) Math.cos(currentAngle) * triangleSize;
        float tipY = centerY + (float) Math.sin(currentAngle) * triangleSize;

        // 4. Calculate the two back corners
        float leftCornerX = centerX + (float) Math.cos(currentAngle + 2.35f) * (triangleSize * 1f);
        float leftCornerY = centerY + (float) Math.sin(currentAngle + 2.35f) * (triangleSize * 1f);

        float rightCornerX = centerX + (float) Math.cos(currentAngle - 2.35f) * (triangleSize * 1f);
        float rightCornerY = centerY + (float) Math.sin(currentAngle - 2.35f) * (triangleSize * 1f);

        // 5. Build Raylib Vector2 structs for the vertices
        Vector2 v1 = new Vector2(tipX, tipY);
        Vector2 v2 = new Vector2(leftCornerX, leftCornerY);
        Vector2 v3 = new Vector2(rightCornerX, rightCornerY);

        drawTriangle(v1, v3, v2, BLUE);
    }
}
