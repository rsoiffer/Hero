#version 330

uniform sampler2D texture_sampler;

in vec2 texCoord;

out vec4 finalColor;

void main() {
    finalColor = texture(texture_sampler, texCoord);
}