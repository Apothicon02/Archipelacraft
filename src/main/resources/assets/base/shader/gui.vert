layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

uniform mat4 model;

out vec3 pos;
out vec3 norm;

void main()
{
    pos = (position+1)/2;
    norm = normal;
    gl_Position = model * vec4(position, 1.0);
}