layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform bool screenSpace;

out vec3 pos;
out vec3 norm;

void main()
{
    pos = position;
    norm = normal;
    if (screenSpace) {
        gl_Position = model * vec4(position, 1.0);
    } else {
        gl_Position = projection * view * model * vec4(position, 1.0);
    }
}