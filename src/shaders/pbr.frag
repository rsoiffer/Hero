#version 330 core
out vec4 FragColor;
in vec3 WorldPos;
in vec2 TexCoords;
in vec3 Normal;
in vec3 Tangent;
in vec3 Bitangent;

// material parameters
uniform sampler2D albedoMap;
uniform sampler2D normalMap;
uniform sampler2D metallicMap;
uniform sampler2D roughnessMap;
uniform sampler2D aoMap;
uniform sampler2D heightMap;

uniform sampler2D shadowMap;
uniform mat4 lightSpaceMatrix;

// lights
uniform vec3 lightPositions[4];
uniform vec3 lightColors[4];

uniform float heightScale;
uniform float heightOffset;
uniform vec3 camPos;

// ----------------------------------------------------------------------------
float ShadowCalculation()
{
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(WorldPos, 1.0);
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;
    vec3 lightDir = normalize(lightPositions[2]);
    float bias = max(0.005 * (1.0 - dot(Normal, lightDir)), 0.0005);
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for(int x = -1; x <= 1; ++x) {
        for(int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth  ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    if(projCoords.z > 1.0)
        shadow = 0.0;
    return shadow;
}

const float PI = 3.14159265359;
// ----------------------------------------------------------------------------
vec4 tex(sampler2D map, vec2 texCoords)
{
    return texture(map, texCoords);
    // return textureLod(map, texCoords, 0.0);
}
// ----------------------------------------------------------------------------
float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a = roughness*roughness;
    float a2 = a*a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH*NdotH;

    float nom   = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return nom / denom;
}
// ----------------------------------------------------------------------------
float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = (roughness + 1.0);
    float k = (r*r) / 8.0;

    float nom   = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return nom / denom;
}
// ----------------------------------------------------------------------------
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}
// ----------------------------------------------------------------------------
vec3 fresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}
// ----------------------------------------------------------------------------
vec2 ParallaxMapping(vec2 texCoords, vec3 viewDir)
{
    // number of depth layers
    const float minLayers = 8;
    const float maxLayers = 32;
    float numLayers = mix(maxLayers, minLayers, abs(dot(Normal, viewDir)));
    // calculate the size of each layer
    float layerDepth = 1.0 / numLayers;
    // depth of current layer
    float currentLayerDepth = 0;
    // the amount to shift the texture coordinates per layer (from vector P)
    vec2 P = viewDir.xy / (viewDir.z + .000001) * heightScale;
    vec2 deltaTexCoords = P / numLayers;

    // get initial values
    vec2  currentTexCoords     = texCoords;
    float currentDepthMapValue = tex(heightMap, currentTexCoords).r - heightOffset;

    while(currentLayerDepth < currentDepthMapValue)
    {
        // shift texture coordinates along direction of P
        currentTexCoords -= deltaTexCoords;
        // get depthmap value at current texture coordinates
        currentDepthMapValue = tex(heightMap, currentTexCoords).r - heightOffset;
        // get depth of next layer
        currentLayerDepth += layerDepth;
    }

    // get texture coordinates before collision (reverse operations)
    vec2 prevTexCoords = currentTexCoords + deltaTexCoords;

    // get depth after and before collision for linear interpolation
    float afterDepth  = currentDepthMapValue - currentLayerDepth;
    float beforeDepth = tex(heightMap, prevTexCoords).r - heightOffset - currentLayerDepth + layerDepth;

    // interpolation of texture coordinates
    float weight = afterDepth / (afterDepth - beforeDepth);
    vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0 - weight);

    return finalTexCoords;
}
// ----------------------------------------------------------------------------
void main()
{
    mat3 TBN = mat3(Tangent, Bitangent, Normal);
    vec3 viewDir = normalize(transpose(TBN) * (camPos - WorldPos));
    vec2 texCoords = ParallaxMapping(TexCoords, viewDir);
    // vec2 texCoords = TexCoords;

    vec3 albedo     = pow(tex(albedoMap, texCoords).rgb, vec3(2.2));
    float metallic  = tex(metallicMap, texCoords).r;
    float roughness = tex(roughnessMap, texCoords).r;
    float ao        = tex(aoMap, texCoords).r;

    vec3 N = normalize(TBN * (texture(normalMap, texCoords).xyz * 2.0 - 1.0));
    vec3 V = normalize(camPos - WorldPos);

    // calculate reflectance at normal incidence; if dia-electric (like plastic) use F0
    // of 0.04 and if it's a metal, use the albedo color as F0 (metallic workflow)
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo, metallic);

    // reflectance equation
    vec3 Lo = vec3(0.0);
    for(int i = 0; i < 4; ++i)
    {
        // calculate per-light radiance
        vec3 L = normalize(lightPositions[i] - WorldPos);
        vec3 H = normalize(V + L);
        float distance = length(lightPositions[i] - WorldPos);
        float attenuation = 1.0 / (distance * distance);
        vec3 radiance = lightColors[i] * attenuation;
        if (i == 2) {
            L = normalize(vec3(.4, .7, 1));
            H = normalize(V + L);
            radiance = vec3(1, .9, .8) * 10 * (1 - ShadowCalculation());
        }
        if (i == 3) {
            L = normalize(vec3(-.4, -.3, .5));
            H = normalize(V + L);
            radiance = vec3(1, .9, .8) * .2;
        }

        // Cook-Torrance BRDF
        float NDF = DistributionGGX(N, H, roughness);
        float G   = GeometrySmith(N, V, L, roughness);
        vec3 F    = fresnelSchlick(max(dot(H, V), 0.0), F0);
        float denominator = 4 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001; // add 0.001 to prevent division by zero
        vec3 specular = NDF * G * F / denominator;
        // kS is equal to Fresnel
        vec3 kS = F;
        // energy conservation
        vec3 kD = vec3(1.0) - kS;
        // only non-metals have diffuse lighting (pure metals have no diffuse light)
        kD *= 1.0 - metallic;
        // scale light by NdotL
        float NdotL = max(dot(N, L), 0.0);
        // add to outgoing radiance Lo
        Lo += (kD * albedo / PI + specular) * radiance * NdotL;  // note that we already multiplied the BRDF by the Fresnel (kS) so we won't multiply by kS again
    }
    // ambient lighting
    vec3 ambient = vec3(0.03) * albedo * ao;
    vec3 color = ambient + Lo;
    // HDR tonemapping
    color = color / (color + vec3(1.0));
    // gamma correct
    color = pow(color, vec3(1.0/2.2));
    // final color
    FragColor = vec4(color, 1.0);
}
