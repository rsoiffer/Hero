#version 330

layout (location=0) in vec2 position_in;
layout (location=1) in vec2 texCoord_in;

out vec2 TexCoords;

void main() {
    gl_Position = vec4(position_in, 0, 1);
    TexCoords = texCoord_in;
}