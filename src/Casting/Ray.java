package Casting;

import java.util.Objects;
import java.util.function.IntBinaryOperator;

import com.raylib.Color;
import static com.raylib.Raylib.GREEN;
import static com.raylib.Raylib.RED;
import static com.raylib.Raylib.drawLine;
import com.raylib.Vector2;

public class Ray {
    private final Vector2 origin;
    private final float direction;
    private float length;
    private Vector2 hitPosition;
    private int hitTileType = -1;
    private boolean hitVerticalSide;
    private boolean hit;

    public Ray(Vector2 origin, float direction) {
        this.origin = Objects.requireNonNull(origin, "origin");
        if (!Float.isFinite(origin.x()) || !Float.isFinite(origin.y()) || !Float.isFinite(direction)) {
            throw new IllegalArgumentException("Ray origin and direction must be finite");
        }
        this.direction = direction;
    }

    /**
     * Casts this ray through a grid map. Distance and map coordinates use tile units.
     *
     * @return true if the ray reached a wall or the map boundary
     */
    public boolean cast(IntBinaryOperator getTile, int wallTileType, float maxDistance) {
        Objects.requireNonNull(getTile, "getTile");
        if (!Float.isFinite(maxDistance) || maxDistance < 0.0f) {
            throw new IllegalArgumentException("maxDistance must be finite and non-negative");
        }

        float rayDirX = (float) Math.cos(direction);
        float rayDirY = (float) Math.sin(direction);
        int mapX = (int) Math.floor(origin.x());
        int mapY = (int) Math.floor(origin.y());
        int stepX = rayDirX < 0.0f ? -1 : 1;
        int stepY = rayDirY < 0.0f ? -1 : 1;
        float deltaDistX = rayDirX == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / rayDirX);
        float deltaDistY = rayDirY == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / rayDirY);
        float sideDistX = rayDirX < 0.0f
                ? (origin.x() - mapX) * deltaDistX
                : (mapX + 1.0f - origin.x()) * deltaDistX;
        float sideDistY = rayDirY < 0.0f
                ? (origin.y() - mapY) * deltaDistY
                : (mapY + 1.0f - origin.y()) * deltaDistY;

        hit = false;
        hitTileType = -1;
        hitVerticalSide = false;
        length = maxDistance;
        hitPosition = pointAt(maxDistance);

        int tileType = getTile.applyAsInt(mapX, mapY);
        if (isSolid(tileType, wallTileType)) {
            return setHit(0.0f, tileType, false, rayDirX, rayDirY);
        }

        while (true) {
            float distance;
            if (sideDistX < sideDistY) {
                distance = sideDistX;
                if (distance > maxDistance) {
                    break;
                }
                mapX += stepX;
                sideDistX += deltaDistX;
                hitVerticalSide = true;
            } else {
                distance = sideDistY;
                if (distance > maxDistance) {
                    break;
                }
                mapY += stepY;
                sideDistY += deltaDistY;
                hitVerticalSide = false;
            }

            tileType = getTile.applyAsInt(mapX, mapY);
            if (isSolid(tileType, wallTileType)) {
                return setHit(distance, tileType, hitVerticalSide, rayDirX, rayDirY);
            }
        }

        return false;
    }

    private boolean isSolid(int tileType, int wallTileType) {
        return tileType == wallTileType || tileType < 0;
    }

    private boolean setHit(float distance, int tileType, boolean verticalSide, float rayDirX, float rayDirY) {
        hit = true;
        length = distance;
        hitTileType = tileType;
        hitVerticalSide = verticalSide;
        hitPosition = new Vector2(
                origin.x() + rayDirX * distance,
                origin.y() + rayDirY * distance);
        return true;
    }

    public Vector2 pointAt(float distance) {
        return new Vector2(
                origin.x() + (float) Math.cos(direction) * distance,
                origin.y() + (float) Math.sin(direction) * distance);
    }

    public Vector2 getOrigin() {
        return origin;
    }

    public float getDirection() {
        return direction;
    }

    public float getLength() {
        return length;
    }

    public Vector2 getHitPosition() {
        return hitPosition;
    }

    public int getHitTileType() {
        return hitTileType;
    }

    public boolean hitVerticalSide() {
        return hitVerticalSide;
    }

    public float getTextureOffset() {
        float coordinate;
        if (hitVerticalSide) {
            // Vertical wall (X-axis boundary): texture scrolls along Y.
            // Flip when facing +X so bricks always read left-to-right from outside.
            coordinate = hitPosition.y();
            if (getDirX() < 0.0f) coordinate = 1.0f - (coordinate - (float) Math.floor(coordinate));
            else                   coordinate = coordinate - (float) Math.floor(coordinate);
        } else {
            // Horizontal wall (Y-axis boundary): texture scrolls along X.
            // Flip when facing -Y so bricks always read left-to-right from outside.
            coordinate = hitPosition.x();
            if (getDirY() > 0.0f) coordinate = 1.0f - (coordinate - (float) Math.floor(coordinate));
            else                   coordinate = coordinate - (float) Math.floor(coordinate);
        }
        return coordinate;
    }

    public boolean hasHit() {
        return hit;
    }

    public void reset() {
        length = 0.0f;
        hitPosition = null;
        hitTileType = -1;
        hitVerticalSide = false;
        hit = false;
    }

    public float Distance() {
        float distance = (float) Math.sqrt(Math.pow(hitPosition.x() - origin.x(), 2) + Math.pow(hitPosition.y() - origin.y(), 2));
        return distance;
    }
  

    public void draw(int mapWidth, int mapHeight) {
        if (!config.Settings.is2DMode && !config.Settings.isMiniMap) {
            return;
        }
        boolean drawMiniMap = !config.Settings.is2DMode && config.Settings.isMiniMap;
        float tileSize = drawMiniMap ? config.Settings.tileSizeMiniMap : config.Settings.tileSize2d;

        float mapOffsetX = drawMiniMap
                ? config.Settings.screenWidth - mapWidth * tileSize
                : (config.Settings.screenWidth - mapWidth * tileSize) / 2.0f;

        float mapOffsetY = drawMiniMap
                ? 0.0f
                : (config.Settings.screenHeight - mapHeight * tileSize) / 2.0f;

        float startX = mapOffsetX + origin.x() * tileSize;
        float startY = mapOffsetY + origin.y() * tileSize;

        float endX = mapOffsetX + hitPosition.x() * tileSize;
        float endY = mapOffsetY + hitPosition.y() * tileSize;

        // 3. Choose color based on wall shading layout rule
        Color lineColor = hitVerticalSide ? RED : GREEN;

        // 4. Draw the pixel-scaled line
        drawLine(Math.round(startX), Math.round(startY), Math.round(endX), Math.round(endY), lineColor);
    }

    public float getDirX() {
        return (float) Math.cos(direction);
    }

    public float getDirY() {
        return (float) Math.sin(direction);
    }
}