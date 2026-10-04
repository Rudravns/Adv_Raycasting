#version 330

in vec2 fragTexCoord;
in vec4 fragColor;

uniform sampler2D texture0;
uniform sampler2D uNormalMap;
uniform vec4 colDiffuse;
uniform vec2 uWorldPosition;
uniform vec2 uSurfaceNormal;
uniform vec2 uCameraPosition;
uniform vec4 uLightPositionRadiusIntensity[2];
uniform vec4 uLightColor[2];
uniform vec4 uLightDirectionAttenuation[2];
uniform int uLightCount;
uniform float uAmbientLight;
uniform float uSpecularStrength;
uniform float uShininess;
uniform float uNormalStrength;

out vec4 finalColor;

float spotlightFactor(int lightIndex, vec2 directionToPoint, float distanceToLight)
{
    if (uLightDirectionAttenuation[lightIndex].z < 0.5 || distanceToLight <= 0.0)
    {
        return 1.0;
    }

    float lightAngle = uLightDirectionAttenuation[lightIndex].x;
    vec2 lightDirection = vec2(cos(lightAngle), sin(lightAngle));
    float directionDot = dot(lightDirection, directionToPoint);
    return smoothstep(cos(radians(45.0)), cos(radians(30.0)), directionDot);
}

void main()
{
    // Start with the wall's brick texel and a small ambient contribution.
    vec4 surface = texture(texture0, fragTexCoord) * colDiffuse * fragColor;
    // Normal maps store tangent-space directions in RGB, encoded from [-1, 1] to [0, 1].
    vec3 tangentNormal = texture(uNormalMap, fragTexCoord).rgb * 2.0 - 1.0;
    vec3 illumination = vec3(uAmbientLight);
    vec3 specular = vec3(0.0);
    vec3 geometricNormal = normalize(vec3(uSurfaceNormal, 0.0));
    // Texture U always increases with world Y on vertical faces and world X on horizontal faces.
    vec3 tangent = abs(uSurfaceNormal.x) > 0.5
        ? vec3(0.0, 1.0, 0.0)
        : vec3(1.0, 0.0, 0.0);
    vec3 bitangent = normalize(cross(geometricNormal, tangent));
    vec3 mappedNormal = normalize(
        tangent * tangentNormal.x +
        bitangent * tangentNormal.y +
        geometricNormal * tangentNormal.z);
    // Blend toward the mapped normal so strength can be tuned without changing the asset.
    vec3 normal = normalize(mix(geometricNormal, mappedNormal, clamp(uNormalStrength, 0.0, 1.0)));
    vec3 surfacePosition = vec3(uWorldPosition, 0.5 - fragTexCoord.y);
    vec3 viewDirection = normalize(vec3(uCameraPosition, 0.0) - surfacePosition);

    for (int i = 0; i < uLightCount; i++)
    {
        vec3 lightPosition = vec3(uLightPositionRadiusIntensity[i].xy, 0.0);
        float lightRadius = uLightPositionRadiusIntensity[i].z;
        float lightIntensity = uLightPositionRadiusIntensity[i].w;
        vec3 lightOffset = lightPosition - surfacePosition;
        float distanceToLight = length(lightOffset);

        if (lightRadius <= 0.0 || distanceToLight >= lightRadius)
        {
            continue;
        }

        vec3 lightDirection = lightOffset / max(distanceToLight, 0.0001);
        float distanceFalloff = pow(
            max(0.0, 1.0 - distanceToLight / lightRadius),
            max(0.1, uLightDirectionAttenuation[i].y));
        float spotlight = spotlightFactor(i, -lightDirection.xy, distanceToLight);
        float strength = distanceFalloff * spotlight * max(0.0, lightIntensity);

        // Diffuse lighting depends on how directly the wall faces the light.
        float diffuse = max(dot(normal, lightDirection), 0.0);
        illumination += uLightColor[i].rgb * diffuse * strength;

        // Add a tight highlight using the halfway vector between light and camera.
        vec3 halfwayDirection = normalize(lightDirection + viewDirection);
        float highlight = pow(max(dot(normal, halfwayDirection), 0.0), max(1.0, uShininess));
        specular += uLightColor[i].rgb * highlight * strength * uSpecularStrength;
    }

    finalColor = vec4(surface.rgb * illumination + specular, surface.a);
}
