uniform ivec2 res;

out vec4 fragColor;

void main() {
    vec2 pos = gl_FragCoord.xy;
    fragColor = vec4(0.5, 0.5, 0.5, 1);
}