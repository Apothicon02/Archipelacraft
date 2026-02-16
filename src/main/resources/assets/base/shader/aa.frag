uniform mat4 projection;
uniform mat4 view;
uniform mat4 prevView;
uniform ivec2 res;
uniform bool taa;
uniform int offsetIdx;
uniform int offsetIdxOld;

uniform layout(binding = 0) sampler2D in_color_old;
uniform layout(binding = 1) sampler2D in_color;

in vec4 gl_FragCoord;

out vec4 fragColor;

const float[16] xOffsets = float[16](0.0f, -0.25f, 0.25f, -0.375f, 0.125f, -0.125f, 0.375f, -0.4375f, 0.0625f, -0.1875f, 0.3125f, -0.3125f, 0.1875f, -0.0625f, 0.4375f, -0.46875f);
const float[16] yOffsets = float[16](0.0f, 0.166667f, -0.388889f, -0.055556f, 0.277778f, -0.277778f, 0.055556f, 0.388889f, -0.462963f, -0.12963f, 0.203704f, -0.351852f, -0.018519f, 0.314815f, -0.240741f, 0.092593f);

vec2 reproject(vec3 worldPos) {
    vec4 projectionVec = projection * prevView * vec4(worldPos, 1.0f);
    projectionVec.xyz /= projectionVec.w;
    projectionVec.xy = projectionVec.xy * 0.5f + 0.5f;
    return projectionVec.xy;
}
vec3 getDir() {
    vec2 screenSpace = (gl_FragCoord.xy+vec2(xOffsets[offsetIdxOld], yOffsets[offsetIdxOld])) / res;
    vec4 clipSpace = vec4(screenSpace * 2.0f - 1.0f, -1.0f, 1.0f);
    vec4 eyeSpace = vec4(vec2(inverse(projection) * clipSpace), -1.0f, 0.0f);
    return normalize(vec3(inverse(view)*eyeSpace));
}

const float nearClip = 0.01f;
void main() {
    vec2 texCoords = gl_FragCoord.xy/res;
    vec4 currentColor = texture(in_color, texCoords);
    if (taa) {
        vec4 oldColorUnjittered = texture(in_color_old, texCoords);
        vec3 worldPos = inverse(view)[3].xyz + (getDir() * (nearClip/oldColorUnjittered.w));
        vec2 reprojected = reproject(worldPos);
        vec4 oldColor = (reprojected.x >= 0.f && reprojected.x < 1.f && reprojected.y >= 0.f && reprojected.y < 1.f) ? texture(in_color_old, reprojected) : currentColor;

        vec3 nearColor0 = texelFetch(in_color, ivec2(gl_FragCoord.x+1, gl_FragCoord.y), 0).rgb;
        vec3 nearColor1 = texelFetch(in_color, ivec2(gl_FragCoord.x, gl_FragCoord.y+1), 0).rgb;
        vec3 nearColor2 = texelFetch(in_color, ivec2(gl_FragCoord.x-1, gl_FragCoord.y), 0).rgb;
        vec3 nearColor3 = texelFetch(in_color, ivec2(gl_FragCoord.x, gl_FragCoord.y-1), 0).rgb;
        vec3 boxMin = min(currentColor.rgb, min(nearColor0, min(nearColor1, min(nearColor2, nearColor3))));
        vec3 boxMax = max(currentColor.rgb, max(nearColor0, max(nearColor1, max(nearColor2, nearColor3))));
        oldColor.rgb = clamp(oldColor.rgb, boxMin, boxMax);

        fragColor.rgb = mix(currentColor.rgb, oldColor.rgb, 0.9f);
        fragColor.w = currentColor.w;
    } else {
        fragColor = currentColor;
    }
}