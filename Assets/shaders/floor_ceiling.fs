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
const float FLOOR_SPECULAR_STRENGTH = 0.75;
const float FLOOR_SHININESS = 32.0;

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
    
    // In raycasting, depth from horizon is perpendicular distance to camera plane.
    // Distance along the ray is perpDistance / cos(rayAngle - uPlayerAngle) to correct fish-eye on the floor.
    float perpDistance = (uResolution.y * 0.5) / max(distanceFromHorizon, 1.0);
    float angleDelta = rayAngle - uPlayerAngle;
    float worldDistance = perpDistance / max(cos(angleDelta), 0.0001);
    vec2 worldPosition = uPlayerPosition + rayDirection * worldDistance;

    bool isCeiling = screenY < horizon;
    vec3 baseColor = isCeiling ? vec3(0.78) : vec3(0.50);
    vec3 illumination = vec3(AMBIENT_LIGHT);
    vec3 specular = vec3(0.0);

    // Surface position in 3D: ceiling is at z = +0.5, floor is at z = -0.5
    float surfaceZ = isCeiling ? 0.5 : -0.5;
    vec3 surfacePos = vec3(worldPosition, surfaceZ);
    vec3 camPos = vec3(uPlayerPosition, 0.0);
    vec3 viewDir = normalize(camPos - surfacePos);
    vec3 surfaceNormal = isCeiling ? vec3(0.0, 0.0, -1.0) : vec3(0.0, 0.0, 1.0);

    for (int i = 0; i < uLightCount; i++)
    {
        vec3 lightPos = vec3(uLightPositionRadius[i].xy, 0.0);
        float lightRadius = uLightPositionRadius[i].z;
        float lightIntensity = uLightPositionRadius[i].w;
        vec3 lightToSurface = surfacePos - lightPos;
        float distanceToLight = length(lightToSurface);

        if (lightRadius > 0.0 && distanceToLight < lightRadius)
        {
            float falloff = pow(1.0 - distanceToLight / lightRadius,
                                max(0.1, uLightDirectionAttenuation[i].y));
            float spotlight = 1.0;
            if (uLightDirectionAttenuation[i].z > 0.5 && distanceToLight > 0.0)
            {
                vec3 lightForward = vec3(cos(uLightDirectionAttenuation[i].x),
                                         sin(uLightDirectionAttenuation[i].x), 0.0);
                float directionDot = dot(lightForward, lightToSurface / distanceToLight);
                spotlight = smoothstep(cos(radians(45.0)), cos(radians(30.0)), directionDot);
            }
            
            float strength = falloff * spotlight * lightIntensity;
            
            // Diffuse lighting
            vec3 lightDir = normalize(lightPos - surfacePos);
            float diffuse = max(dot(surfaceNormal, lightDir), 0.0);
            illumination += uLightColor[i].rgb * diffuse * strength;

            // Specular gloss reflection on floor and ceiling
            if (diffuse > 0.0)
            {
                vec3 halfDir = normalize(lightDir + viewDir);
                float nDotH = max(dot(surfaceNormal, halfDir), 0.0);
                float highlight = pow(nDotH, FLOOR_SHININESS);
                specular += uLightColor[i].rgb * highlight * strength * FLOOR_SPECULAR_STRENGTH;
            }
        }
    }

    finalColor = vec4(clamp(baseColor * illumination + specular, 0.0, 1.0), 1.0) * fragColor;
}

