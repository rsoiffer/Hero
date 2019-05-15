#version 330 core

in vec2 TexCoords;

out vec4 FragColor;

// G-buffer
uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform sampler2D gAlbedo;
uniform sampler2D gMRA;
uniform sampler2D gEmissive;

// lights
uniform vec3 sunColor;
uniform vec3 sunDirection;

const int NUM_CASCADES = 5;
uniform sampler2DShadow shadowMap[NUM_CASCADES];
uniform mat4 lightSpaceMatrix[NUM_CASCADES];
uniform float cascadeEndClipSpace[NUM_CASCADES];

uniform sampler2D brdfLUT;

uniform mat4 projectionViewMatrix;
uniform vec3 camPos;

const float PI = 3.14159265359;
// ----------------------------------------------------------------------------
float ShadowCalculation(vec3 FragPos, vec3 Normal)
{
    vec4 proj = projectionViewMatrix * vec4(FragPos, 1);
    float projZ = proj.z / proj.w;

    float shadow = 0.0;
    for (int i = 0; i < NUM_CASCADES; i++) {
        if (projZ <= cascadeEndClipSpace[i]) {
            vec4 fragPosLightSpace = lightSpaceMatrix[i] * vec4(FragPos, 1.0);
            vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
            projCoords = projCoords * 0.5 + 0.5;
            vec3 lightDir = normalize(sunDirection);
            float bias = max(0.01 * (1.0 - dot(Normal, lightDir)), 0.001);
            float currentDepth = projCoords.z - bias;
            shadow = texture(shadowMap[i], vec3(projCoords.xy, currentDepth));
            if (i < NUM_CASCADES - 1) {
                float z1 = log(1 - cascadeEndClipSpace[i]);
                float z2 = log(1 - cascadeEndClipSpace[i+1]);
                float amt = 10 * (log(1 - projZ) - z1) / (z1 - z2);
                if (amt < 1) {
                    vec4 fragPosLightSpace2 = lightSpaceMatrix[i+1] * vec4(FragPos, 1.0);
                    vec3 projCoords2 = fragPosLightSpace2.xyz / fragPosLightSpace2.w;
                    projCoords2 = projCoords2 * 0.5 + 0.5;
                    float currentDepth2 = projCoords2.z - bias;
                    float shadow2 = texture(shadowMap[i+1], vec3(projCoords2.xy, currentDepth2));
                    shadow = mix(shadow2, shadow, amt);
                }
            }
            break;
        }
    }
    return shadow;
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
vec3 FresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}
// ----------------------------------------------------------------------------
vec3 CalculateLighting(vec3 Normal, vec3 Albedo, float Metallic, float Roughness,
                        vec3 viewDir, vec3 F0, vec3 L, vec3 radiance)
{
    vec3 H        = normalize(viewDir + L);
    float NDF     = DistributionGGX(Normal, H, Roughness);
    float G       = GeometrySmith(Normal, viewDir, L, Roughness);
    vec3 kS       = FresnelSchlick(max(dot(H, viewDir), 0.0), F0);
    vec3 specular = NDF * G * kS / (4 * max(dot(Normal, viewDir), 0.0) * max(dot(Normal, L), 0.0) + 0.001);
    vec3 kD       = (vec3(1.0) - kS) * (1.0 - Metallic);
    float NdotL   = max(dot(Normal, L), 0.0);
    return (kD * Albedo / PI + specular) * radiance * NdotL;
}
// ----------------------------------------------------------------------------
vec3 FakeIrradiance(vec3 dir, float roughness)
{
    float x = tanh(cos(dir.z) / (roughness + .01));
    return mix(vec3(.4, .7, 1), vec3(.3, .3, .3), x) * .15;
}
// ----------------------------------------------------------------------------
vec3 rgb2hsv(vec3 c)
{
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}
// ----------------------------------------------------------------------------
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
// ----------------------------------------------------------------------------
void main()
{
    vec3 FragPos = texture(gPosition, TexCoords).rgb;
    vec3 Normal = texture(gNormal, TexCoords).rgb;
    vec3 Albedo = texture(gAlbedo, TexCoords).rgb;
    float Metallic = texture(gMRA, TexCoords).r;
    float Roughness = texture(gMRA, TexCoords).g;
    float AO = texture(gMRA, TexCoords).b;
    vec3 Emissive = texture(gEmissive, TexCoords).rgb;

    if (length(Normal) == 0) discard;
    vec3 viewDir = normalize(camPos - FragPos);
    vec3 reflectDir = reflect(-viewDir, Normal);
    vec3 F0 = mix(vec3(0.04), Albedo, Metallic);
    vec3 color = Emissive / (2 * PI);
    if (dot(Normal, viewDir) < 0) {
        Normal = -Normal;
    }

    // sun lighting
    vec3 radiance = sunColor * (1 - ShadowCalculation(FragPos, Normal));
    color += CalculateLighting(Normal, Albedo, Metallic, Roughness, viewDir, F0, sunDirection, radiance);

    // real ambient IBL
    /*vec3 kS = FresnelSchlick(max(dot(Normal, viewDir), 0.0), F0);
    vec3 kD = (vec3(1.0) - kS) * (1.0 - Metallic);
    vec3 irradiance = texture(irradianceMap, Normal).rgb;
    vec3 diffuse = kD * irradiance * Albedo;
    const float MAX_REFLECTION_LOD = 4.0;
    vec3 prefilteredColor = textureLod(prefilterMap, R, Roughness * MAX_REFLECTION_LOD).rgb;
    vec2 envBRDF  = texture(brdfLUT, vec2(max(dot(Normal, viewDir), 0.0), Roughness)).rg;
    vec3 specular = prefilteredColor * (kS * envBRDF.x + envBRDF.y);
    color += (diffuse + specular) * AO;*/

    // fake ambient IBL
    vec3 kS = FresnelSchlick(max(dot(Normal, viewDir), 0.0), F0);
    vec3 kD = (vec3(1.0) - kS) * (1.0 - Metallic);
    vec3 irradiance = FakeIrradiance(Normal, 1);
    vec3 diffuse = kD * irradiance * Albedo;
    vec3 prefilteredColor = FakeIrradiance(reflectDir, Roughness);
    vec2 envBRDF  = texture(brdfLUT, vec2(max(dot(Normal, viewDir), 0.0), Roughness)).rg;
    vec3 specular = prefilteredColor * (kS * envBRDF.x + envBRDF.y);
    color += (diffuse + specular) * AO;

    // simple ambient lighting
    // color += vec3(0.03) * Albedo * AO;

    // color correction

    // color = max(vec3(0.0), color - 0.004);
    // color = (color*(6.2*color+.5))/(color*(6.2*color+1.7)+0.06);

    color = rgb2hsv(color);
    color = vec3(color.xy, color.z / (1 + color.z));
    color = hsv2rgb(color);
    color = pow(color, vec3(1.0/2.2));

    // color = color / (color + vec3(1.0));
    // color = pow(color, vec3(1.0/2.2));

    FragColor = vec4(color, 1.0);
}
