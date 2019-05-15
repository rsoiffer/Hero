#version 330 core

in vec3 FragPos;
in vec2 TexCoords;
in vec3 Normal;
in vec3 Tangent;
in vec3 Bitangent;

layout (location = 0) out vec3 gPosition;
layout (location = 1) out vec3 gNormal;
layout (location = 2) out vec3 gAlbedo;
layout (location = 3) out vec3 gMRA;
layout (location = 4) out vec3 gEmissive;

// material parameters
uniform sampler2D albedoMap;
uniform sampler2D normalMap;
uniform sampler2D metallicMap;
uniform sampler2D roughnessMap;
uniform sampler2D aoMap;
uniform sampler2D heightMap;
uniform sampler2D alphaMap;
uniform sampler2D emissiveMap;

uniform vec3 camPos;
uniform float lod;

uniform float heightScale = 0.01;
uniform float heightOffset = 0.5;

// ----------------------------------------------------------------------------
float rand(vec2 co)
{
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}
// ----------------------------------------------------------------------------
vec2 ParallaxMapping(vec2 texCoords, vec3 N, vec3 viewDir)
{
    // number of depth layers
    const float minLayers = 8;
    const float maxLayers = 32;
    float numLayers = mix(maxLayers, minLayers, min(abs(dot(N, viewDir)), 1));
    // calculate the size of each layer
    float layerDepth = 1.0 / numLayers;
    // depth of current layer
    float currentLayerDepth = heightOffset;
    // the amount to shift the texture coordinates per layer (from vector P)
    vec2 P = viewDir.xy / (viewDir.z + .000001) * heightScale;
    vec2 deltaTexCoords = P / numLayers;

    // get initial values
    vec2  currentTexCoords     = texCoords;
    float currentDepthMapValue = texture(heightMap, currentTexCoords).r;

    while(currentLayerDepth > currentDepthMapValue)
    {
        // shift texture coordinates along direction of P
        currentTexCoords -= deltaTexCoords;
        // get depthmap value at current texture coordinates
        currentDepthMapValue = texture(heightMap, currentTexCoords).r;
        // get depth of next layer
        currentLayerDepth -= layerDepth;
    }

    // get texture coordinates before collision (reverse operations)
    vec2 prevTexCoords = currentTexCoords + deltaTexCoords;

    // get depth after and before collision for linear interpolation
    float afterDepth  = currentDepthMapValue - currentLayerDepth;
    float beforeDepth = texture(heightMap, prevTexCoords).r - (currentLayerDepth + layerDepth);

    // interpolation of texture coordinates
    float weight = afterDepth / (afterDepth - beforeDepth);
    vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0 - weight);

    return finalTexCoords;
}
// ----------------------------------------------------------------------------
void main()
{
    if (texture(alphaMap, TexCoords).r < .5) {
        discard;
    }
    if (floor(rand(gl_FragCoord.xy) + lod) != 0) {
        discard;
    }

    vec3 T = normalize(Tangent);
    vec3 N = normalize(Normal);
    T = normalize(T - dot(T, N) * N);
    vec3 B = cross(N, T);
    mat3 TBN = mat3(T, B, N);
    // mat3 TBN = mat3(Tangent, Bitangent, Normal);

    vec3 viewDir = normalize(transpose(TBN) * (camPos - FragPos));
    vec2 texCoords = ParallaxMapping(TexCoords, N, viewDir);

    gPosition = FragPos;
    gNormal = normalize(TBN * (texture(normalMap, texCoords).xyz * 2.0 - 1.0));
    gAlbedo = texture(albedoMap, texCoords).rgb;
    gMRA.r = texture(metallicMap, texCoords).r;
    gMRA.g = texture(roughnessMap, texCoords).r;
    gMRA.b = texture(aoMap, texCoords).r;
    gEmissive = texture(emissiveMap, texCoords).rgb;
}
