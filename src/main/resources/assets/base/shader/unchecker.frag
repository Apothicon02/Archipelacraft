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
    ivec2 pos = ivec2(gl_FragCoord.xy);
    bool checkerOn = checker(ivec2(gl_FragCoord.x*8, gl_FragCoord.y));
    bool firstHalf = bool(pos.x < res.x/2);
    if (!((firstHalf && !checkerOn) || (!firstHalf && checkerOn))) {
        pos.y++;
    }
    if (!firstHalf) {
        pos.x -= res.x/2;
    }
    fragColor = texelFetch(in_color, ivec3(pos, 0), 0);
}