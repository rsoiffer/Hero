#version 330

uniform vec4 color;
uniform bool outline;
uniform sampler2D texture_sampler;

in  vec2 texCoord;
out vec4 finalColor;

void main() {
    if (outline) {
        finalColor = color * vec4(1, 1, 1, texture(texture_sampler, texCoord).a);
    } else {
        finalColor = color * vec4(1, 1, 1, texture(texture_sampler, texCoord).r);
    }
}