uniform layout(binding = 0) sampler3D texture;

uniform vec4 color;
uniform int tex;
uniform ivec2 atlasOffset;
uniform vec3 sun;
uniform vec3 mun;
uniform bool alwaysUpfront;

in vec3 pos;
in vec3 norm;
in vec3 wPos;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 rasterPos;
layout (location = 2) out vec4 rasterNorm;
layout (location = 3) out float rasterDepth;

void main() {
    rasterDepth = alwaysUpfront ? 1.f : gl_FragCoord.z;
    rasterPos = vec4(wPos, rasterDepth);
    rasterNorm = vec4(norm, 0);
    if (tex <= 0) {
        fragColor = color;
//        if (max(fragColor.r, max(fragColor.g, fragColor.b)) < 1f) {
//            vec3 source = mun.y > sun.y ? mun : sun;
//            source.y = max(source.y, 500);
//            float brightness = dot(norm.xy, source.xy)*0.0005f;
//            fragColor.rgb *= max(0.3f, 1.f+brightness);
//            if (brightness < 0.f) {
//                fragColor.a += 10;
//            }
//        }
    } else {
        vec4 guiColor = texelFetch(texture, ivec3(atlasOffset.x+(pos.x*16), atlasOffset.y+(abs(1-pos.y)*16), 0), 0)*color;
        if (guiColor.a > 0) {
            fragColor = guiColor;
        } else {
            discard;
        }
    }
}