layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform int offsetIdx;
uniform ivec2 res;
uniform bool taa;

out vec3 pos;
out vec3 norm;
out vec3 wPos;

const float[16] xOffsets = float[16](0.0f, -0.25f, 0.25f, -0.375f, 0.125f, -0.125f, 0.375f, -0.4375f, 0.0625f, -0.1875f, 0.3125f, -0.3125f, 0.1875f, -0.0625f, 0.4375f, -0.46875f);
const float[16] yOffsets = float[16](0.0f, 0.166667f, -0.388889f, -0.055556f, 0.277778f, -0.277778f, 0.055556f, 0.388889f, -0.462963f, -0.12963f, 0.203704f, -0.351852f, -0.018519f, 0.314815f, -0.240741f, 0.092593f);

void main() {
    float xOff = taa ? xOffsets[offsetIdx]/res.x : 0;
    float yOff = taa ? yOffsets[offsetIdx]/res.y : 0;
    pos = position;
    norm = normal;
    vec4 worldPos = model * vec4(position.xy+vec2(xOff, yOff), position.z, 1.0);
    vec4 clipPos = projection * view * worldPos;
    gl_Position = clipPos;

    wPos = worldPos.xyz;
}