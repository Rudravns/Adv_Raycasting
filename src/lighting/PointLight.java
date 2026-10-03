package lighting;

import com.raylib.Color;

public class PointLight {
    public static final float DEFAULT_INTENSITY = 1.0f;
    public static final float DEFAULT_ATTENUATION = 2.0f;
    public static final float OMNIDIRECTIONAL_ANGLE = Float.NaN;

    public float x, y;       // Position on your 2D grid map
    public float radius;     // How far the light travels before fading completely
    public Color color;      // The color tint of the light source
    public float intensity;  // How strong the light is (0.0 to 1.0)
    public float attenuation; // Exponent controlling the distance falloff
    public float angle;       // Spotlight direction in radians; NaN means omnidirectional

    public PointLight(float x, float y, float radius, Color color) {
        this(x, y, radius, color, DEFAULT_INTENSITY, DEFAULT_ATTENUATION, OMNIDIRECTIONAL_ANGLE);
    }

    public PointLight(float x, float y, float radius, Color color, float intensity, float attenuation, float angle) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.intensity = intensity;
        this.attenuation = attenuation;
        this.angle = angle;
    }

    public PointLight(float x, float y, float radius, Color color, float intensity, float attenuation) {
        this(x, y, radius, color, intensity, attenuation, OMNIDIRECTIONAL_ANGLE);
    }

    public PointLight(float x, float y, float radius, Color color, float intensity) {
        this(x, y, radius, color, intensity, DEFAULT_ATTENUATION, OMNIDIRECTIONAL_ANGLE);
    }
}
