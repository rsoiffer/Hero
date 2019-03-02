#version 330

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

layout (location=0) in vec3 position_in;
layout (location=1) in vec2 texCoord_in;

out vec2 texCoord;

void main() {
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position_in, 1.0);
    texCoord = texCoord_in;
}