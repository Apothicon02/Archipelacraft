uniform vec4 color;
uniform vec3 sun;
uniform vec3 mun;

in vec3 pos;
in vec3 norm;
out vec4 fragColor;

void main() {
    fragColor = color;
    if (fragColor.a < 2 && fragColor.a > 0) {
        vec3 source = mun.y > sun.y ? mun : sun;
        source.y = max(source.y, 72);
        float brightness = dot(norm.xy, source.xy)*0.0005f;
        fragColor.rgb *= max(0.3f, 1.f+brightness);
        if (brightness < 0.f) {
            fragColor.a += 10;
        }
    }
}