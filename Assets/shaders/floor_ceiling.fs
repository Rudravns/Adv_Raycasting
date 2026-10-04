#version 330
#define MAX_POINT_LIGHTS 32

in vec2 fragTexCoord;
in vec4 fragColor;

uniform vec2  uResolution;
uniform vec2  uPlayerPosition;
uniform float uPlayerAngle;
uniform float uFieldOfView;
uniform vec4  uLightPositionRadius[MAX_POINT_LIGHTS];
uniform vec4  uLightColor[MAX_POINT_LIGHTS];
uniform vec4  uLightDirectionAttenuation[MAX_POINT_LIGHTS];
uniform int   uLightCount;

out vec4 finalColor;

const float AMBIENT_LIGHT     = 0.15;
const float SPECULAR_STRENGTH = 0.35;  // floor/ceiling gloss intensity
const float SHININESS         = 16.0;  // tighter = smaller hotspot

void main()
{
    // Raylib's gl_FragCoord Y starts at the bottom; convert to top-down pixels.
    float screenY  = uResolution.y - gl_FragCoord.y;
    float horizon  = uResolution.y * 0.5;
    float distFromHorizon = abs(screenY - horizon);
    bool  isFloor  = screenY > horizon;

    // ── World-position reconstruction ────────────────────────────────────────
    float screenX    = gl_FragCoord.x / uResolution.x;
    float deltaAngle = (screenX - 0.5) * uFieldOfView;
    float rayAngle   = uPlayerAngle + deltaAngle;
    vec2  rayDir     = vec2(cos(rayAngle), sin(rayAngle));

    // Fish-eye correction: use perpendicular (cosine) distance, not diagonal.
    float perpDist   = (uResolution.y * 0.5) / max(distFromHorizon, 1.0);
    float worldDist  = perpDist / max(cos(deltaAngle), 0.0001);
    vec2  worldPos   = uPlayerPosition + rayDir * worldDist;

    // ── Base colour ───────────────────────────────────────────────────────────
    // Ceiling lighter, floor slightly darker.
    vec3 baseColor = isFloor ? vec3(0.50) : vec3(0.78);

    // ── Lighting ──────────────────────────────────────────────────────────────
    vec3 illumination = vec3(AMBIENT_LIGHT);
    vec3 specular     = vec3(0.0);

    // Floor/ceiling normals point straight up (+Z) or down (-Z) in world space.
    vec3 surfaceNormal = isFloor ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 0.0, -1.0);
    // Camera is at player position, at eye height (z = 0 in our flat world).
    vec3 surfacePos3  = vec3(worldPos, 0.0);
    vec3 viewDir      = normalize(vec3(uPlayerPosition - worldPos, perpDist));

    for (int i = 0; i < uLightCount; i++)
    {
        vec2  lightPos2  = uLightPositionRadius[i].xy;
        float lightRadius = uLightPositionRadius[i].z;
        float lightIntensity = uLightPositionRadius[i].w;
        float dist2D     = distance(worldPos, lightPos2);

        if (lightRadius <= 0.0 || dist2D >= lightRadius) continue;

        float falloff = pow(max(0.0, 1.0 - dist2D / lightRadius),
                            max(0.1, uLightDirectionAttenuation[i].y));

        // Spotlight cone test (same math as wall shader).
        float spotlight = 1.0;
        if (uLightDirectionAttenuation[i].z > 0.5 && dist2D > 0.0)
        {
            vec2  lightFwd  = vec2(cos(uLightDirectionAttenuation[i].x),
                                   sin(uLightDirectionAttenuation[i].x));
            vec2  toSample  = (worldPos - lightPos2) / dist2D;
            spotlight = smoothstep(cos(radians(45.0)), cos(radians(30.0)),
                                   dot(lightFwd, toSample));
        }

        float strength = falloff * spotlight * max(0.0, lightIntensity);

        // For flat surfaces the diffuse is simply the upward/downward component.
        // Light is at z = 0 (player height), surface at z = 0 too, so
        // the light direction in 3D has no Z contribution — diffuse is flat.
        illumination += uLightColor[i].rgb * strength;

        // Specular: treat the light as a point above/below the flat surface.
        vec3 lightPos3 = vec3(lightPos2, 0.0);
        vec3 lightDir3 = normalize(lightPos3 - surfacePos3);
        vec3 halfVec   = normalize(lightDir3 + viewDir);
        float highlight = pow(max(dot(surfaceNormal, halfVec), 0.0), SHININESS);

        // Schlick Fresnel — glossy rim at glancing angles.
        float cosV   = max(dot(surfaceNormal, viewDir), 0.0);
        float fresnel = 0.04 + 0.96 * pow(1.0 - cosV, 5.0);

        specular += uLightColor[i].rgb * highlight * (SPECULAR_STRENGTH + fresnel * 0.5) * strength;
    }

    vec3 lit = clamp(baseColor * illumination + specular, 0.0, 1.0);
    finalColor = vec4(lit, 1.0) * fragColor;
}
