#version 330

in vec2 fragTexCoord;
in vec4 fragColor;

uniform vec2 uResolution;
uniform vec2 uPlayerPosition;
uniform float uPlayerAngle;
uniform float uFieldOfView;
uniform vec4 uLightPositionRadius[2];
uniform vec4 uLightColor[2];
uniform vec4 uLightDirectionAttenuation[2];
uniform int uLightCount;

out vec4 finalColor;

const float AMBIENT_LIGHT = 0.15;

void main()
{
    // Raylib's screen-space Y origin is at the bottom; convert it to top-down pixels.
    float screenY = uResolution.y - gl_FragCoord.y;
    float horizon = uResolution.y * 0.5;
    float distanceFromHorizon = abs(screenY - horizon);

    // Reconstruct this pixel's world position from its horizontal ray and vertical depth.
    float screenX = gl_FragCoord.x / uResolution.x;
    float rayAngle = uPlayerAngle + (screenX - 0.5) * uFieldOfView;
    vec2 rayDirection = vec2(cos(rayAngle), sin(rayAngle));
    float worldDistance = (uResolution.y * 0.5) / max(distanceFromHorizon, 1.0);
    vec2 worldPosition = uPlayerPosition + rayDirection * worldDistance;

    // Give the ceiling a lighter base than the floor, then add ambient and point lighting.
    vec3 baseColor = screenY < horizon ? vec3(0.78) : vec3(0.50);
    vec3 illumination = vec3(AMBIENT_LIGHT);

    for (int i = 0; i < uLightCount; i++)
    {
        vec2 lightPosition = uLightPositionRadius[i].xy;
        float lightRadius = uLightPositionRadius[i].z;
        float lightIntensity = uLightPositionRadius[i].w;
        float distanceToLight = distance(worldPosition, lightPosition);

        if (lightRadius > 0.0 && distanceToLight < lightRadius)
        {
            float falloff = pow(1.0 - distanceToLight / lightRadius,
                                max(0.1, uLightDirectionAttenuation[i].y));
            float spotlight = 1.0;
            if (uLightDirectionAttenuation[i].z > 0.5 && distanceToLight > 0.0)
            {
                vec2 lightDirection = vec2(cos(uLightDirectionAttenuation[i].x),
                                           sin(uLightDirectionAttenuation[i].x));
                vec2 directionToSample = (worldPosition - lightPosition) / distanceToLight;
                float directionDot = dot(lightDirection, directionToSample);
                spotlight = smoothstep(cos(radians(45.0)), cos(radians(30.0)), directionDot);
            }
            illumination += uLightColor[i].rgb * falloff * spotlight * lightIntensity;
        }
    }

    finalColor = vec4(clamp(baseColor * illumination, 0.0, 1.0), 1.0) * fragColor;
}
