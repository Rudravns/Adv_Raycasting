
import com.raylib.Vector2;

public class Map {
    private int width;
    private int height;
    private int[][] tiles;

    public Map(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new int[width][height];

        // Initialize the map with default tile type (e.g., 0)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    tiles[x][y] = 1; // Set border tiles to type 1
                } else {
                    tiles[x][y] = 0; // Default tile type
                }
            }
        }

    }

    public void setupMap(Starter_maps starterMap) {
        float[][] mapData = starterMap.getMapData();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = (int) mapData[y][x]; // Note the order of indices
            }
        }
    }
    

    public void setTile(int x, int y, int tileType) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            tiles[x][y] = tileType;
        }
    }

    public int getTile(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return tiles[x][y];
        }
        return -1; // Return -1 for out of bounds
    }

    public Vector2 getPlayerSpawnPoint() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y] == Map.TileType.PLAYER.getValue()) { 
                    return new Vector2(x, y);
                }
            }
        }
        return null; // Return null if no spawn point is found
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Vector2 getTilePosition(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return new Vector2(x, y);
        }
        return null; // Return null for out of bounds
    }

    public Vector2 convertToTileCoordinates(float worldX, float worldY, int tileSize) {
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);
        return new Vector2(tileX, tileY);
    }

    public static enum TileType {
        EMPTY(0),
        WALL(1),
        PLAYER(2);

        private final int value;

        TileType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static enum Direction {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    public static enum Starter_maps {
        // 1. Enum constants must come first and pass the required argument
        MAP1(new float[][]{
            {1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f},
            {1f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 1f},
            {1f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 1f},
            {1f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 1f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f},
            {1f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 0f, 1f},
            {1f, 1f, 1f, 0f, 1f, 0f, 0f, 0f, 2f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f},
            {1f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 1f},
            {1f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f},
            {1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f}
        }),
        MAP2(new float[][]{
            {1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 1f},
            {1f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f}, 
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 1f},
            {1f, 0f, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 0f, 1f},
            {1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f},
            {1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f}
        });


        

        // 2. Field declarations
        private final float[][] mapData;

        // 3. Parameterized constructor
        Starter_maps(float[][] mapData) {
            this.mapData = mapData;
        }

        // 4. Getter method
        public float[][] getMapData() {
            return mapData;
        }

    }
}