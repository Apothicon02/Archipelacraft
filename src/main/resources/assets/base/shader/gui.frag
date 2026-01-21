uniform vec4 color;

uniform layout(binding = 0) sampler2D scene_color;
uniform layout(binding = 1) sampler3D gui;
uniform layout(binding = 2) sampler3D item;
uniform int tex;
uniform int layer;
uniform ivec2 atlasOffset;
uniform ivec2 offset;
uniform ivec2 size;
uniform ivec2 scale;
uniform ivec2 res;

in vec3 pos;

out vec4 fragColor;

void main() {
    if (color.a == -1f) {
        fragColor = texture(scene_color, pos.xy, 0);
    } else {
        vec4 guiColor = texelFetch(tex == 0 ? gui : item, ivec3(atlasOffset.x+(pos.x*(size.x-0.01f)), atlasOffset.y+(abs(1-pos.y)*(size.y-0.01f)), layer), 0)*color;
        if (guiColor.a > 0) {
            vec4 sceneColor = texelFetch(scene_color, ivec2(pos.xy*scale)+offset, 0);
            fragColor = vec4(mix(sceneColor.rgb, guiColor.rgb, guiColor.a), 1.f);
        } else {
            discard;
        }
    }
}