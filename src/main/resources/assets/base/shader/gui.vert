layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

uniform mat4 model;

out vec3 pos;

void main()
{
    pos = (position+1)/2;
    gl_Position = model * vec4(position, 1.0);
}