uniform layout(binding = 0) sampler3D texture;

uniform vec4 color;
uniform int tex;
uniform ivec2 atlasOffset;
uniform vec3 sun;
uniform vec3 mun;

in vec3 pos;
in vec3 norm;
out vec4 fragColor;

void main() {
    if (tex == 0) {
        fragColor = color;
        if (fragColor.a < 2 && fragColor.a > 0) {
            vec3 source = mun.y > sun.y ? mun : sun;
            source.y = max(source.y, 500);
            float brightness = dot(norm.xy, source.xy)*0.0005f;
            fragColor.rgb *= max(0.3f, 1.f+brightness);
            if (brightness < 0.f) {
                fragColor.a += 10;
            }
        }
    } else {
        vec4 guiColor = texelFetch(texture, ivec3(atlasOffset.x+(pos.x*16), atlasOffset.y+(abs(1-pos.y)*16), 0), 0)*color;
        if (guiColor.a > 0) {
            fragColor = guiColor;
        } else {
            discard;
        }
    }
}