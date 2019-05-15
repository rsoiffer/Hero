#version 330 core

in vec2 TexCoords;

// material parameters
uniform sampler2D alphaMap;

uniform float lod;

// ----------------------------------------------------------------------------
float rand(vec2 co)
{
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
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
}
