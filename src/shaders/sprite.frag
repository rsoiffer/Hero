#version 330

uniform vec4 color;
uniform sampler2D texture_sampler;

in  vec2 texCoord;
out vec4 finalColor;

void main() {
    finalColor = color * texture(texture_sampler, texCoord);
}