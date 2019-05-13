#version 330 core

in vec3 FragPos;
in vec2 TexCoords;
in vec3 Normal;

layout (location = 0) out vec3 gPosition;
layout (location = 1) out vec3 gNormal;
layout (location = 2) out vec3 gAlbedo;
layout (location = 3) out vec3 gMRA;
layout (location = 4) out vec3 gEmissive;

// material parameters
uniform sampler2D diffuseMap;
uniform float metallic;
uniform float roughness;

void main()
{
    gPosition = FragPos;
    gNormal = normalize(Normal);
    gAlbedo = texture(diffuseMap, TexCoords).rgb;
    gMRA = vec3(metallic, roughness, 1);
    gEmissive = vec3(0, 0, 0);
}
