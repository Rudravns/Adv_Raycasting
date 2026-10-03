package lighting;

import static com.raylib.Raylib.beginShaderMode;
import static com.raylib.Raylib.drawRectangle;
import static com.raylib.Raylib.endShaderMode;
import static com.raylib.Raylib.getShaderLocation;
import static com.raylib.Raylib.loadShader;
import static com.raylib.Raylib.setShaderValue;
import static com.raylib.Raylib.setShaderValueV;
import static com.raylib.Raylib.unloadShader;

import com.raylib.Raylib;
import com.raylib.Shader;
import com.raylib.Vector2;
import config.Settings;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

public final class FloorCeilingShader {
    private static final String SHADER_PATH = "Assets/shaders/floor_ceiling.fs";
    private static final int LIGHT_COUNT = 2;
    private static final int FLOAT = Raylib.ShaderUniformDataType.SHADER_UNIFORM_FLOAT;
    private static final int VEC2 = Raylib.ShaderUniformDataType.SHADER_UNIFORM_VEC2;
    private static final int VEC4 = Raylib.ShaderUniformDataType.SHADER_UNIFORM_VEC4;
    private static final int INT = Raylib.ShaderUniformDataType.SHADER_UNIFORM_INT;

    private Shader shader;
    private int resolutionLocation;
    private int playerPositionLocation;
    private int playerAngleLocation;
    private int fieldOfViewLocation;
    private int lightPositionRadiusLocation;
    private int lightColorLocation;
    private int lightDirectionAttenuationLocation;
    private int lightCountLocation;

    public void load() {
        if (shader != null) {
            return;
        }

        shader = loadShader("", SHADER_PATH);
        resolutionLocation = getShaderLocation(shader, "uResolution");
        playerPositionLocation = getShaderLocation(shader, "uPlayerPosition");
        playerAngleLocation = getShaderLocation(shader, "uPlayerAngle");
        fieldOfViewLocation = getShaderLocation(shader, "uFieldOfView");
        lightPositionRadiusLocation = getShaderLocation(shader, "uLightPositionRadius");
        lightColorLocation = getShaderLocation(shader, "uLightColor");
        lightDirectionAttenuationLocation = getShaderLocation(shader, "uLightDirectionAttenuation");
        lightCountLocation = getShaderLocation(shader, "uLightCount");

        if (resolutionLocation < 0 || playerPositionLocation < 0 || playerAngleLocation < 0
                || fieldOfViewLocation < 0 || lightPositionRadiusLocation < 0
                || lightColorLocation < 0 || lightDirectionAttenuationLocation < 0
                || lightCountLocation < 0) {
            unloadShader(shader);
            shader = null;
            throw new IllegalStateException("Floor/ceiling shader is missing a required uniform");
        }
    }

    public void draw(Vector2 playerPosition, float playerAngle, PointLight... lights) {
        if (shader == null) {
            throw new IllegalStateException("Floor/ceiling shader must be loaded before drawing");
        }
        Objects.requireNonNull(playerPosition, "playerPosition");
        Objects.requireNonNull(lights, "lights");
        if (lights.length > LIGHT_COUNT) {
            throw new IllegalArgumentException("Floor/ceiling shader supports at most two lights");
        }

        // Pack each light into a vec4: position XY plus radius, and normalized RGBA.
        float[] lightPositionRadius = new float[LIGHT_COUNT * 4];
        float[] lightColors = new float[LIGHT_COUNT * 4];
        float[] lightDirectionAttenuation = new float[LIGHT_COUNT * 4];
        for (int i = 0; i < lights.length; i++) {
            PointLight light = Objects.requireNonNull(lights[i], "light");
            int offset = i * 4;
            lightPositionRadius[offset] = light.x;
            lightPositionRadius[offset + 1] = light.y;
            lightPositionRadius[offset + 2] = light.radius;
            lightPositionRadius[offset + 3] = Math.max(0.0f, light.intensity);
            lightColors[offset] = Byte.toUnsignedInt(light.color.r()) / 255.0f;
            lightColors[offset + 1] = Byte.toUnsignedInt(light.color.g()) / 255.0f;
            lightColors[offset + 2] = Byte.toUnsignedInt(light.color.b()) / 255.0f;
            lightColors[offset + 3] = Byte.toUnsignedInt(light.color.a()) / 255.0f;
            lightDirectionAttenuation[offset] = Float.isNaN(light.angle) ? 0.0f : light.angle;
            lightDirectionAttenuation[offset + 1] = Math.max(0.1f, light.attenuation);
            lightDirectionAttenuation[offset + 2] = Float.isNaN(light.angle) ? 0.0f : 1.0f;
        }

        // Uniform buffers only need to stay alive until Raylib copies them to the GPU.
        try (Arena arena = Arena.ofConfined()) {
            Vector2 resolution = new Vector2(Settings.screenWidth, Settings.screenHeight);
            Vector2 playerPositionValue = new Vector2(playerPosition.x(), playerPosition.y());
            setShaderValue(shader, resolutionLocation, resolution.memorySegment, VEC2);
            setShaderValue(shader, playerPositionLocation, playerPositionValue.memorySegment, VEC2);
            setShaderValue(shader, playerAngleLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, playerAngle), FLOAT);
            setShaderValue(shader, fieldOfViewLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, (float) Math.toRadians(Settings.fov)), FLOAT);
            setShaderValueV(shader, lightPositionRadiusLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, lightPositionRadius), VEC4, LIGHT_COUNT);
            setShaderValueV(shader, lightColorLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, lightColors), VEC4, LIGHT_COUNT);
            setShaderValueV(shader, lightDirectionAttenuationLocation,
                    arena.allocateFrom(ValueLayout.JAVA_FLOAT, lightDirectionAttenuation), VEC4, LIGHT_COUNT);
            setShaderValue(shader, lightCountLocation,
                    arena.allocateFrom(ValueLayout.JAVA_INT, lights.length), INT);

            // The shader uses each fragment's screen coordinate to reconstruct its floor/ceiling world point.
            beginShaderMode(shader);
            drawRectangle(0, 0, Settings.screenWidth, Settings.screenHeight, Raylib.WHITE);
            endShaderMode();
        }
    }

    public void unload() {
        if (shader != null) {
            unloadShader(shader);
            shader = null;
        }
    }
}
