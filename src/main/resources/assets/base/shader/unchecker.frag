uniform layout(binding = 0) sampler2D in_color;

in vec4 gl_FragCoord;

out vec4 fragColor;

void main() {
    fragColor = texelFetch(in_color, ivec2(gl_FragCoord.x/2, gl_FragCoord.y), 0);
//    vec4 offsetColor = texelFetch(in_color, ivec2((gl_FragCoord.x-1)/2, gl_FragCoord.y), 0);
//    ivec4 offsetColorLod = ivec4(offsetColor*10);
//    vec4 neighborColor = texelFetch(in_color, ivec2(gl_FragCoord.x/2, gl_FragCoord.y-1), 0);
//    ivec4 neighborColorLod = ivec4(neighborColor*10);
//    if (offsetColorLod == neighborColorLod) {
//        fragColor = offsetColor;
//    } else {
//        neighborColor = texelFetch(in_color, ivec2(gl_FragCoord.x/2, gl_FragCoord.y+1), 0);
//        neighborColorLod = ivec4(neighborColor*10);
//        if (offsetColorLod == neighborColorLod) {
//            fragColor = offsetColor;
//        }
//    }
}