package Casting;

import static com.raylib.Raylib.*;
import com.raylib.Color;
import com.raylib.Raylib;
import com.raylib.Rectangle;
import com.raylib.Shader;
import com.raylib.Texture;
import com.raylib.Vector2;
import config.Settings;
import lighting.LightingManager; // Import your new package here!
import lighting.PointLight;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

public class Wall {

    private final java.util.ArrayList<WallSlice> walls = new java.util.ArrayList<>();
    private Texture texture;
    private Texture normalTexture;
    private Texture heightTexture;
    private Texture ambientOcclusionTexture;
    private Texture roughnessTexture;
    private Shader specularShader;
    private int worldPositionLocation;
    private int surfaceNormalLocation;
    private int cameraPositionLocation;
    private int lightPositionRadiusIntensityLocation;
    private int lightColorLocation;
    private int lightDirectionAttenuationLocation;
    private int lightCountLocation;
    private int ambientLightLocation;
    private int specularStrengthLocation;
    private int shininessLocation;
    private int normalMapLocation;
    private int normalStrengthLocation;
    private int heightMapLocation;
    private int ambientOcclusionMapLocation;
    private int roughnessMapLocation;
    private int heightScaleLocation;

    // We can use base solid colors now since the lighting manager handles side dimming automatically!
    private final Color verticalTint = LIGHTGRAY;
    private final Color horizontalTint = RAYWHITE;

    public void loadTexture() {
        if (texture != null) {
            return;
        }
        try {
            texture = loadMaterialMap("Assets/materials/brickWall/albedo.png");
            normalTexture = loadMaterialMap("Assets/materials/brickWall/normal.png");
            heightTexture = loadMaterialMap("Assets/materials/brickWall/height.png");
            ambientOcclusionTexture = loadMaterialMap("Assets/materials/brickWall/ao.png");
            roughnessTexture = loadMaterialMap("Assets/materials/brickWall/roughness.png");

            specularShader = com.raylib.Raylib.loadShader("", "Assets/shaders/specular.fs");
            worldPositionLocation = getShaderLocation(specularShader, "uWorldPosition");
            surfaceNormalLocation = getShaderLocation(specularShader, "uSurfaceNormal");
            cameraPositionLocation = getShaderLocation(specularShader, "uCameraPosition");
            lightPositionRadiusIntensityLocation = getShaderLocation(specularShader, "uLightPositionRadiusIntensity");
            lightColorLocation = getShaderLocation(specularShader, "uLightColor");
            lightDirectionAttenuationLocation = getShaderLocation(specularShader, "uLightDirectionAttenuation");
            lightCountLocation = getShaderLocation(specularShader, "uLightCount");
            ambientLightLocation = getShaderLocation(specularShader, "uAmbientLight");
            specularStrengthLocation = getShaderLocation(specularShader, "uSpecularStrength");
            shininessLocation = getShaderLocation(specularShader, "uShininess");
            normalMapLocation = getShaderLocation(specularShader, "uNormalMap");
            normalStrengthLocation = getShaderLocation(specularShader, "uNormalStrength");
            heightMapLocation = getShaderLocation(specularShader, "uHeightMap");
            ambientOcclusionMapLocation = getShaderLocation(specularShader, "uAmbientOcclusionMap");
            roughnessMapLocation = getShaderLocation(specularShader, "uRoughnessMap");
            heightScaleLocation = getShaderLocation(specularShader, "uHeightScale");

            if (worldPositionLocation < 0 || surfaceNormalLocation < 0 || cameraPositionLocation < 0
                    || lightPositionRadiusIntensityLocation < 0 || lightColorLocation < 0
                    || lightDirectionAttenuationLocation < 0 || lightCountLocation < 0
                    || ambientLightLocation < 0 || specularStrengthLocation < 0 || shininessLocation < 0
                    || normalMapLocation < 0 || normalStrengthLocation < 0 || heightMapLocation < 0
                    || ambientOcclusionMapLocation < 0 || roughnessMapLocation < 0 || heightScaleLocation < 0) {
                throw new IllegalStateException("Specular wall shader is missing a required uniform");
            }
        } catch (RuntimeException exception) {
            unloadTexture();
            throw exception;
        }
    }

    private static Texture loadMaterialMap(String path) {
        Texture materialMap = com.raylib.Raylib.loadTexture(path);
        if (materialMap.width() <= 0 || materialMap.height() <= 0) {
            com.raylib.Raylib.unloadTexture(materialMap);
            throw new IllegalStateException("Could not load wall material map: " + path);
        }
        return materialMap;
    }

    public void unloadTexture() {
        if (texture != null) {
            com.raylib.Raylib.unloadTexture(texture);
            texture = null;
        }
        if (normalTexture != null) {
            com.raylib.Raylib.unloadTexture(normalTexture);
            normalTexture = null;
        }
        if (heightTexture != null) {
            com.raylib.Raylib.unloadTexture(heightTexture);
            heightTexture = null;
        }
        if (ambientOcclusionTexture != null) {
            com.raylib.Raylib.unloadTexture(ambientOcclusionTexture);
            ambientOcclusionTexture = null;
        }
        if (roughnessTexture != null) {
            com.raylib.Raylib.unloadTexture(roughnessTexture);
            roughnessTexture = null;
        }
        if (specularShader != null) {
            com.raylib.Raylib.unloadShader(specularShader);
            specularShader = null;
        }
    }

    // UPDATE: Accept hitX and hitY world positions directly from your DDA raycaster
    public void addWall(float rayLength, boolean isVertical, float textureOffset,
            float hitX, float hitY, float normalX, float normalY) {
        float safeDistance = Math.max(rayLength, 0.0001f);
        float height = (Settings.screenHeight / safeDistance) + Settings.wallHeight;
        walls.add(new WallSlice(height, isVertical, textureOffset, hitX, hitY, normalX, normalY));
    }

    public void clearWalls() {
        walls.clear();
    }

    public void draw(Vector2 cameraPosition, PointLight... lights) {
        if (texture == null) {
            throw new IllegalStateException("Wall texture must be loaded before drawing walls");
        }
        Objects.requireNonNull(cameraPosition, "cameraPosition");
        Objects.requireNonNull(lights, "lights");
        if (lights.length > Settings.MAX_POINT_LIGHTS) {
            throw new IllegalArgumentException(
                    "Wall shader supports at most " + Settings.MAX_POINT_LIGHTS + " lights");
        }
        float columnWidth = (float) Settings.screenWidth / Settings.numRays;
        if (Settings.simpleWallColors) {
            drawSimpleColors(columnWidth);
            return;
        }

        int lightCount = lights.length;
        float[] lightPositionRadiusIntensity = new float[Settings.MAX_POINT_LIGHTS * 4];
        float[] lightColors = new float[Settings.MAX_POINT_LIGHTS * 4];
        float[] lightDirectionAttenuation = new float[Settings.MAX_POINT_LIGHTS * 4];
        for (int i = 0; i < lightCount; i++) {
            PointLight light = Objects.requireNonNull(lights[i], "light");
            int offset = i * 4;
            lightPositionRadiusIntensity[offset] = light.x;
            lightPositionRadiusIntensity[offset + 1] = light.y;
            lightPositionRadiusIntensity[offset + 2] = light.radius;
            lightPositionRadiusIntensity[offset + 3] = light.intensity;
            lightColors[offset] = Byte.toUnsignedInt(light.color.r()) / 255.0f;
            lightColors[offset + 1] = Byte.toUnsignedInt(light.color.g()) / 255.0f;
            lightColors[offset + 2] = Byte.toUnsignedInt(light.color.b()) / 255.0f;
            lightColors[offset + 3] = Byte.toUnsignedInt(light.color.a()) / 255.0f;
            lightDirectionAttenuation[offset] = Float.isNaN(light.angle) ? 0.0f : light.angle;
            lightDirectionAttenuation[offset + 1] = light.attenuation;
            lightDirectionAttenuation[offset + 2] = Float.isNaN(light.angle) ? 0.0f : 1.0f;
        }

        try (Arena arena = Arena.ofConfined()) {
            var worldPosition = arena.allocate(2 * Float.BYTES);
            var surfaceNormal = arena.allocate(2 * Float.BYTES);
            var cameraPositionValue = arena.allocate(2 * Float.BYTES);
            var ambientLight = arena.allocate(ValueLayout.JAVA_FLOAT);
            var specularStrength = arena.allocate(ValueLayout.JAVA_FLOAT);
            var shininess = arena.allocate(ValueLayout.JAVA_FLOAT);

            cameraPositionValue.setAtIndex(ValueLayout.JAVA_FLOAT, 0, cameraPosition.x());
            cameraPositionValue.setAtIndex(ValueLayout.JAVA_FLOAT, 1, cameraPosition.y());
            ambientLight.set(ValueLayout.JAVA_FLOAT, 0, 0.15f);
            specularStrength.set(ValueLayout.JAVA_FLOAT, 0, Settings.WALL_SPECULAR_STRENGTH);
            shininess.set(ValueLayout.JAVA_FLOAT, 0, Settings.WALL_SHININESS);

            setShaderValueTexture(specularShader, normalMapLocation, normalTexture);
            setShaderValue(specularShader, normalStrengthLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, Settings.WALL_NORMAL_STRENGTH),
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_FLOAT);
            setShaderValue(specularShader, heightScaleLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, Settings.WALL_HEIGHT_SCALE),
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_FLOAT);
            setShaderValueV(specularShader, lightPositionRadiusIntensityLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, lightPositionRadiusIntensity),
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_VEC4, Settings.MAX_POINT_LIGHTS);
            setShaderValueV(specularShader, lightColorLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, lightColors),
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_VEC4, Settings.MAX_POINT_LIGHTS);
            setShaderValueV(specularShader, lightDirectionAttenuationLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, lightDirectionAttenuation),
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_VEC4, Settings.MAX_POINT_LIGHTS);
            setShaderValue(specularShader, lightCountLocation,
                    arena.allocateFrom(ValueLayout.JAVA_INT, lightCount),
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_INT);
            setShaderValue(specularShader, cameraPositionLocation, cameraPositionValue,
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_VEC2);
            setShaderValue(specularShader, ambientLightLocation, ambientLight,
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_FLOAT);
            setShaderValue(specularShader, specularStrengthLocation, specularStrength,
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_FLOAT);
            setShaderValue(specularShader, shininessLocation, shininess,
                    Raylib.ShaderUniformDataType.SHADER_UNIFORM_FLOAT);

            for (int i = 0; i < walls.size(); i++) {
                WallSlice wall = walls.get(i);
                float x = i * columnWidth;
                float y = (Settings.screenHeight - wall.height) / 2.0f;
                float textureX = Math.min(texture.width() - 1,
                        (float) Math.floor(wall.textureOffset * texture.width()));
                Rectangle source = new Rectangle(textureX, 0.0f, 1.0f, texture.height());
                Rectangle destination = new Rectangle(x, y, columnWidth + 1.0f, wall.height);

                worldPosition.setAtIndex(ValueLayout.JAVA_FLOAT, 0, wall.hitX);
                worldPosition.setAtIndex(ValueLayout.JAVA_FLOAT, 1, wall.hitY);
                surfaceNormal.setAtIndex(ValueLayout.JAVA_FLOAT, 0, wall.normalX);
                surfaceNormal.setAtIndex(ValueLayout.JAVA_FLOAT, 1, wall.normalY);
                setShaderValue(specularShader, worldPositionLocation, worldPosition,
                        Raylib.ShaderUniformDataType.SHADER_UNIFORM_VEC2);
                setShaderValue(specularShader, surfaceNormalLocation, surfaceNormal,
                        Raylib.ShaderUniformDataType.SHADER_UNIFORM_VEC2);

                // Ending the shader mode flushes this slice before the next slice changes its uniforms.
                // Re-bind the normal map inside every beginShaderMode call because endShaderMode
                // resets active texture units in Raylib.
                beginShaderMode(specularShader);
                setShaderValueTexture(specularShader, normalMapLocation, normalTexture);
                setShaderValueTexture(specularShader, heightMapLocation, heightTexture);
                setShaderValueTexture(specularShader, ambientOcclusionMapLocation, ambientOcclusionTexture);
                setShaderValueTexture(specularShader, roughnessMapLocation, roughnessTexture);
                drawTexturePro(texture, source, destination,
                        new Vector2(0.0f, 0.0f), 0.0f, WHITE);
                endShaderMode();
            }
        }
    }

    private void drawSimpleColors(float columnWidth) {
        for (int i = 0; i < walls.size(); i++) {
            WallSlice wall = walls.get(i);
            float x = i * columnWidth;
            float y = (Settings.screenHeight - wall.height) / 2.0f;
            Color tint = LightingManager.calculateLighting(
                    wall.isVertical ? verticalTint : horizontalTint,
                    wall.hitX, wall.hitY, wall.isVertical);
            drawRectangle((int) x, (int) y, (int) Math.ceil(columnWidth + 1.0f),
                    (int) Math.ceil(wall.height), tint);
        }
    }

    private static final class WallSlice {

        private final float height;
        private final boolean isVertical;
        private final float textureOffset;
        private final float hitX;
        private final float hitY;
        private final float normalX;
        private final float normalY;

        private WallSlice(float height, boolean isVertical, float textureOffset,
                float hitX, float hitY, float normalX, float normalY) {
            this.height = height;
            this.isVertical = isVertical;
            this.textureOffset = textureOffset;
            this.hitX = hitX;
            this.hitY = hitY;
            this.normalX = normalX;
            this.normalY = normalY;
        }
    }
}
