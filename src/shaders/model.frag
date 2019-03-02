#version 330

uniform vec4 color;
uniform vec3 cameraPos;
uniform float time;

in vec3 fragColor;
in float fragOcclusion;
in float fragFog;
in vec3 fragPos;

out vec4 finalColor;

void main() {
    float correctedOcclusion = pow(fragOcclusion, 2.2);
    vec3 newFragColor = fragColor;
    newFragColor = newFragColor * color.rgb * correctedOcclusion;
    newFragColor = mix(newFragColor, vec3(0.4, 0.7, 1.0), fragFog);
    finalColor = vec4(newFragColor, color.a);
}