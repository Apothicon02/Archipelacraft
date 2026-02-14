layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform int offsetIdx;
uniform ivec2 res;

out vec3 pos;
out vec3 norm;
out vec3 wPos;

const float[16] xOffsets = float[16](0.0f, -0.5f, 0.5f, -0.75f, 0.25f, -0.25f, 0.75f, -0.875f, 0.125f, -0.375f, 0.625f, -0.625f, 0.375f, -0.125f, 0.875f, -0.9375f);
const float[16] yOffsets = float[16](-0.333334f, 0.333334f, -0.777778f, -0.111112f, 0.555556f, -0.555556f, 0.111112f, 0.777778f, -0.925926f, -0.25926f, 0.407408f, -0.703704f, -0.037038f, 0.62963f, -0.481482f, 0.185186f);

void main()
{
    float xOff = xOffsets[offsetIdx]/res.x;
    float yOff = yOffsets[offsetIdx]/res.y;
    pos = position;
    norm = normal;
    vec4 worldPos = model * vec4(position, 1.0);
    wPos = worldPos.xyz;
    vec4 clipPos = projection * view * worldPos;
    //clipPos += vec4(xOff, yOff, 0, 0)*clipPos.w;
    gl_Position = clipPos;
}