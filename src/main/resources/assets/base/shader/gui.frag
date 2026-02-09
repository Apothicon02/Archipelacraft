uniform vec4 color;

uniform layout(binding = 0) sampler2D scene_color;
uniform layout(binding = 1) sampler2D blurred;
uniform layout(binding = 2) sampler3D gui;
uniform layout(binding = 3) sampler3D item;
uniform int tex;
uniform int layer;
uniform ivec2 atlasOffset;
uniform ivec2 offset;
uniform ivec2 size;
uniform ivec2 scale;
uniform ivec2 res;
uniform bool tiltShift;
uniform bool dof;

in vec3 pos;

out vec4 fragColor;

void main() {
    if (color.a == -1f) {
        vec4 baseColor = texture(scene_color, pos.xy, 0);
        vec4 blurColor = texture(blurred, gl_FragCoord.xy/res.xy, 0);
        vec4 blurriness = vec4(tiltShift ? max(0, sqrt(sqrt(abs(pos.y-0.5)*2))-0.5f)*2.f : 0); //tilt-shift
        blurriness = max(blurriness, vec4(dof ? mix(1, 0, clamp(baseColor.a*500, 0, 1)) : 0)); //dof
        fragColor = mix(baseColor, blurColor, min(vec4(1), blurriness));
        fragColor = mix(fragColor, max(fragColor, blurColor), min(vec4(1), blurColor)); //bloom
    } else {
        vec4 guiColor = texelFetch(tex == 0 ? gui : item, ivec3(atlasOffset.x+(pos.x*size.x), atlasOffset.y+(abs(1-pos.y)*size.y), layer), 0)*color;
        if (guiColor.a > 0) {
            vec4 sceneColor = texelFetch(scene_color, ivec2(pos.xy*scale)+offset, 0);
            fragColor = vec4(mix(sceneColor.rgb, guiColor.rgb, guiColor.a), 1.f);
        } else {
            discard;
        }
    }
}