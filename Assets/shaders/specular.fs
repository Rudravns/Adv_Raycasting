#version 330
#define MAX_POINT_LIGHTS 32

in vec2 fragTexCoord;
in vec4 fragColor;

uniform sampler2D texture0;
uniform sampler2D uNormalMap;
uniform sampler2D uHeightMap;
uniform sampler2D uAmbientOcclusionMap;
uniform sampler2D uRoughnessMap;
uniform vec4 colDiffuse;
uniform vec2 uWorldPosition;
uniform vec2 uSurfaceNormal;
uniform vec2 uCameraPosition;
uniform vec4 uLightPositionRadiusIntensity[MAX_POINT_LIGHTS];
uniform vec4 uLightColor[MAX_POINT_LIGHTS];
uniform vec4 uLightDirectionAttenuation[MAX_POINT_LIGHTS];
uniform int uLightCount;
uniform float uAmbientLight;
uniform float uSpecularStrength;
uniform float uShininess;
uniform float uNormalStrength;
uniform float uHeightScale;

out vec4 finalColor;

// Returns the spotlight cone factor in [0, 1] using a true 3D cone test.
// lightForward is the light's look direction in 3D; dirToSurface is the normalised vector from light to surface.
float spotlightFactor3D(int idx, vec3 dirToSurface)
{
    if (uLightDirectionAttenuation[idx].z < 0.5)
        return 1.0; // Not a spotlight — omni light.

    float lightAngle = uLightDirectionAttenuation[idx].x;
    vec3 lightForward = vec3(cos(lightAngle), sin(lightAngle), 0.0);
    float cosTheta = dot(lightForward, dirToSurface);
    // Outer cone 45°, inner cone 30° (smooth edge).
    return smoothstep(cos(radians(45.0)), cos(radians(30.0)), cosTheta);
}

void main()
{
    // The supplied height panel uses bright bricks and dark mortar.
    vec3 geometricNormal = normalize(vec3(uSurfaceNormal, 0.0));
    vec3 bitangent = vec3(0.0, 0.0, 1.0);
    vec3 tangent = abs(uSurfaceNormal.x) > 0.5
        ? vec3(0.0, 1.0, 0.0)
        : vec3(1.0, 0.0, 0.0);
    vec3 surfacePosition = vec3(uWorldPosition, 0.5 - fragTexCoord.y);
    vec3 viewDirection = normalize(vec3(uCameraPosition, 0.0) - surfacePosition);

    // Shift the material samples along the view direction for a low-cost depth illusion.
    vec3 tangentView = vec3(
        dot(viewDirection, tangent),
        dot(viewDirection, bitangent),
        dot(viewDirection, geometricNormal));
    vec2 materialUv = fragTexCoord;
    float baseHeight = texture(uHeightMap, materialUv).r;
    materialUv -= tangentView.xy / max(abs(tangentView.z), 0.15)
        * ((baseHeight - 0.5) * uHeightScale);

    vec4 surface = texture(texture0, materialUv) * colDiffuse * fragColor;
    float ambientOcclusion = texture(uAmbientOcclusionMap, materialUv).r;
    float roughness = texture(uRoughnessMap, materialUv).r;

    // Decode the tangent-space normal, then transform it into the wall's world basis.
    vec3 tangentNormal = texture(uNormalMap, materialUv).rgb * 2.0 - 1.0;
    tangentNormal.xy *= uNormalStrength;
    tangentNormal = normalize(tangentNormal);
    tangentNormal.y = -tangentNormal.y;
    vec3 mappedNormal = normalize(
        tangent    * tangentNormal.x +
        bitangent  * tangentNormal.y +
        geometricNormal * tangentNormal.z);
    vec3 normal = mappedNormal;

    // AO affects ambient fill; direct lights retain their full contribution.
    vec3 illumination = vec3(uAmbientLight * mix(0.35, 1.0, ambientOcclusion));
    vec3 specular     = vec3(0.0);

    for (int i = 0; i < uLightCount; i++)
    {
        vec3  lightPos       = vec3(uLightPositionRadiusIntensity[i].xy, 0.0);
        float lightRadius    = uLightPositionRadiusIntensity[i].z;
        float lightIntensity = uLightPositionRadiusIntensity[i].w;
        vec3  lightOffset    = lightPos - surfacePosition;
        float dist           = length(lightOffset);

        if (lightRadius <= 0.0 || dist >= lightRadius) continue;

        vec3  lightDir      = lightOffset / max(dist, 0.0001);
        float distFalloff   = pow(max(0.0, 1.0 - dist / lightRadius),
                                  max(0.1, uLightDirectionAttenuation[i].y));
        float spotlight     = spotlightFactor3D(i, -lightDir); // -lightDir == from light toward surface
        float strength      = distFalloff * spotlight * max(0.0, lightIntensity);

        // Diffuse (Lambertian)
        float diffuse = max(dot(normal, lightDir), 0.0);
        illumination += uLightColor[i].rgb * diffuse * strength;

        // Specular (Blinn-Phong)
        vec3  halfVec   = normalize(lightDir + viewDirection);
        float roughExponent = mix(max(1.0, uShininess * 0.12),
                                  max(1.0, uShininess),
                                  1.0 - clamp(roughness, 0.0, 1.0));
        float highlight = pow(max(dot(normal, halfVec), 0.0), roughExponent);

        // Schlick Fresnel — adds a rim gloss when surface grazes the camera.
        float cosView  = max(dot(normal, viewDirection), 0.0);
        float fresnel  = 0.04 + 0.96 * pow(1.0 - cosView, 5.0);

        float gloss = (uSpecularStrength + fresnel * 0.15)
            * (1.0 - 0.75 * clamp(roughness, 0.0, 1.0));
        specular += uLightColor[i].rgb * highlight * gloss * strength;
    }

    finalColor = vec4(surface.rgb * illumination + specular, surface.a);
}
