uniform ivec2 res;

uniform layout(binding = 0) sampler3D in_color;

in vec4 gl_FragCoord;
out vec4 fragColor;

bool checker(ivec2 pixel) {
    bool xOdd = bool(pixel.x % 2 == 1);
    bool yOdd = bool(pixel.y % 2 == 1);
    if ((xOdd && yOdd) || (!xOdd && !yOdd)) { //both even or both odd
        return true;
    }
    return false;
}

void main() {
    bool yOdd = bool(int(gl_FragCoord.y) % 2 == 1);
    ivec2 pos = ivec2((gl_FragCoord.x/2) + (yOdd ? 1 : 0), gl_FragCoord.y);
    fragColor = texelFetch(in_color, ivec3(pos, 0), 0);
}