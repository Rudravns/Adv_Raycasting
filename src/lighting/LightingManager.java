package lighting;

import com.raylib.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class LightingManager {

    private static final ArrayList<PointLight> lights = new ArrayList<>();
    private static final int CACHE_CAPACITY = 32768;
    private static final int POSITION_QUANTIZATION = 8;
    private static final Map<LightingCacheKey, Color> lightingCache = new LinkedHashMap<>(
            CACHE_CAPACITY, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LightingCacheKey, Color> eldest) {
            return size() > CACHE_CAPACITY;
        }
    };

    // Low baseline light so the map is never pure pitch black
    private static final float AMBIENT_LIGHT = 0.15f;

    public static void addLight(PointLight light) {
        lights.add(light);
        lightingCache.clear();
    }

    public static void updateLightPosition(PointLight light, float x, float y) {
        int previousXCell = Math.round(light.x * POSITION_QUANTIZATION);
        int previousYCell = Math.round(light.y * POSITION_QUANTIZATION);
        int nextXCell = Math.round(x * POSITION_QUANTIZATION);
        int nextYCell = Math.round(y * POSITION_QUANTIZATION);
        light.x = x;
        light.y = y;
        if (previousXCell != nextXCell || previousYCell != nextYCell) {
            lightingCache.clear();
        }
    }

    public static void updateLightDirection(PointLight light, float angle) {
        if (Float.floatToIntBits(light.angle) != Float.floatToIntBits(angle)) {
            light.angle = angle;
            lightingCache.clear();
        }
    }

    public static void clearLights() {
        lights.clear();
        lightingCache.clear();
    }

    public static Color calculateLighting(Color baseColor, float hitX, float hitY, boolean isVertical) {
        int baseR = Byte.toUnsignedInt(baseColor.r());
        int baseG = Byte.toUnsignedInt(baseColor.g());
        int baseB = Byte.toUnsignedInt(baseColor.b());
        int sampleX = Math.round(hitX * POSITION_QUANTIZATION);
        int sampleY = Math.round(hitY * POSITION_QUANTIZATION);
        LightingCacheKey key = new LightingCacheKey(
                sampleX, sampleY, baseR, baseG, baseB,
                Byte.toUnsignedInt(baseColor.a()), isVertical);
        Color cachedColor = lightingCache.get(key);
        if (cachedColor != null) {
            return cachedColor;
        }

        float lightX = sampleX / (float) POSITION_QUANTIZATION;
        float lightY = sampleY / (float) POSITION_QUANTIZATION;
        float accumulatedR = baseR * AMBIENT_LIGHT;
        float accumulatedG = baseG * AMBIENT_LIGHT;
        float accumulatedB = baseB * AMBIENT_LIGHT;

        for (PointLight light : lights) {
            float dx = lightX - quantize(light.x);
            float dy = lightY - quantize(light.y);
            float distanceSquared = dx * dx + dy * dy;
            float radiusSquared = light.radius * light.radius;

            if (distanceSquared >= radiusSquared) {
                continue;
            }

            float distance = (float) Math.sqrt(distanceSquared);
            float distanceFalloff = Math.max(0.0f, 1.0f - distance / light.radius);
            float intensity = (float) Math.pow(distanceFalloff, Math.max(0.1f, light.attenuation));
            if (!Float.isNaN(light.angle) && distance > 0.0f) {
                float directionX = (float) Math.cos(light.angle);
                float directionY = (float) Math.sin(light.angle);
                float directionDot = (directionX * dx + directionY * dy) / distance;
                intensity *= smoothstep((float) Math.cos(Math.toRadians(45.0)),
                        (float) Math.cos(Math.toRadians(30.0)), directionDot);
            }
            intensity *= Math.max(0.0f, light.intensity);
            accumulatedR += (baseR / 255.0f) * Byte.toUnsignedInt(light.color.r()) * intensity;
            accumulatedG += (baseG / 255.0f) * Byte.toUnsignedInt(light.color.g()) * intensity;
            accumulatedB += (baseB / 255.0f) * Byte.toUnsignedInt(light.color.b()) * intensity;
        }

        // Apply a directional 30% shadow penalty to vertical walls
        float sideMultiplier = isVertical ? 0.7f : 1.0f;
        accumulatedR *= sideMultiplier;
        accumulatedG *= sideMultiplier;
        accumulatedB *= sideMultiplier;

        // Clamp the final channels safely to the byte boundary (0 to 255)
        byte r = (byte) Math.min(255, Math.max(0, (int) Math.round(accumulatedR)));
        byte g = (byte) Math.min(255, Math.max(0, (int) Math.round(accumulatedG)));
        byte b = (byte) Math.min(255, Math.max(0, (int) Math.round(accumulatedB)));

        Color result = new Color(r, g, b, baseColor.a());
        lightingCache.put(key, result);
        return result;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0.0f, Math.min(1.0f, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }

    private static float quantize(float value) {
        return Math.round(value * POSITION_QUANTIZATION) / (float) POSITION_QUANTIZATION;
    }

    private record LightingCacheKey(
            int x, int y, int red, int green, int blue, int alpha, boolean isVertical) {
    }
}
