#version 330 core

in vec2 TexCoords;

// material parameters
uniform sampler2D alphaMap;

void main()
{
    if (texture(alphaMap, TexCoords).r < .5) {
        discard;
    }
}
