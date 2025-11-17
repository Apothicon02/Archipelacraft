int size = 1024;
int height = 320;

uniform mat4 projection;
uniform mat4 view;
uniform ivec3 selected;
uniform bool ui;
uniform ivec2 res;

uniform layout(binding = 0) sampler2D raster_color;
uniform layout(binding = 1) sampler2D raster_depth;
uniform layout(binding = 2, rgba32f) image3D atlas;
uniform layout(binding = 3, rgba16i) iimage3D blocks;

in vec4 gl_FragCoord;

out vec4 fragColor;

vec3 stepMask(vec3 sideDist) {
    bvec3 b1 = lessThan(sideDist.xyz, sideDist.yzx);
    bvec3 b2 = lessThanEqual(sideDist.xyz, sideDist.zxy);
    bvec3 mask = bvec3(
        b1.x && b2.x,
        b1.y && b2.y,
        b1.z && b2.z
    );
    if(!any(mask)) {
        mask.z = true;
    }

    return vec3(mask);
}

bool checker(ivec2 pixel) {
    bool xOdd = bool(pixel.x % 2 == 1);
    bool yOdd = bool(pixel.y % 2 == 1);
    if ((xOdd && yOdd) || (!xOdd && !yOdd)) { //both even or both odd
        return true;
    }
    return false;
}

ivec2 getBlock(float x, float y, float z) {
    return imageLoad(blocks, ivec3(int(x), int(y), int(z))).rg;
}

bool didntTraceAnything = true;
bool hitSelection = false;
float voxelBrightness = 0.f;
vec3 mapPos = vec3(0);

vec4 raytrace(vec3 rayPos, vec3 rayDir) {
    mapPos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((mapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);
    vec3 prevMapPos = mapPos+(stepMask(sideDist+(mask*(-raySign)*deltaDist))*(-raySign));

    for (int i = 0; i < size*2; i++) {
        if (distance(rayPos, mapPos) > size) {
            break;
        }
        ivec2 block = getBlock(mapPos.x, mapPos.y, mapPos.z);
        vec4 voxelColor = block.x == 1 ? vec4(0, 0, 1, 0.2f) : (block.x == 2 ? vec4(0, 1, 0, 1) : vec4(0));
        if (checker(ivec2(mapPos.x, mapPos.z))) {
            voxelColor.rgb *= 0.75f;
        }
        if (voxelColor.a > 0.f) {
            voxelBrightness = max(voxelColor.r, max(voxelColor.g, voxelColor.b));
            if (selected == mapPos) {
                hitSelection = true;
            }
            vec3 normal = ivec3(mapPos - prevMapPos);
            if (normal.y >0) { //down
                voxelColor *= 0.7f;
            } else if (normal.y <0) { //up
                voxelColor *= 1.f;
            } else if (normal.z >0) { //south
                voxelColor *= 0.85f;
            } else if (normal.z <0) { //north
                voxelColor *= 0.85f;
            } else if (normal.x >0) { //west
                voxelColor *= 0.75f;
            } else if (normal.x <0) { //east
                voxelColor *= 0.95f;
            }
            vec3 uv3d = vec3(0);
            vec3 intersect = vec3(0);
            vec3 mini = ((mapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float blockDist = max(mini.x, max(mini.y, mini.z));
            intersect = rayPos + rayDir*blockDist;
            uv3d = intersect - mapPos;

            if (mapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - mapPos;
            }
            mapPos += uv3d;
            didntTraceAnything = false;
            return voxelColor;
        }

        mask = stepMask(sideDist);
        prevMapPos = mapPos;
        mapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return mapPos.y < 64 ? vec4(0.f, 0.f, 1.f, 1.f) : vec4(0.5f, 0.75f, 1.f, 1.f);
}

float nearClip = 0.1f;

void main() {
    vec2 pos = gl_FragCoord.xy;//window space
    vec4 rasterColor = texture(raster_color, pos/res);
    float rasterDepth = texture(raster_depth, pos/res).r;
    fragColor = vec4(rasterDepth);
    vec2 uv = ((pos / res)*2.f)-1.f;//ndc clip space
    vec4 clipSpace = (inverse(projection) * vec4(uv, -1.f, 1.f));//world space
    clipSpace.w = 0;
    vec3 ogDir = normalize((inverse(view)*clipSpace).xyz);
    vec3 ogPos = inverse(view)[3].xyz;
    if (ui && uv.x >= -0.004f && uv.x <= 0.004f && uv.y >= -0.004385f && uv.y <= 0.004385f) {
        fragColor = vec4(0.9, 0.9, 1, 1);
    } else {
        fragColor = raytrace(ogPos, ogDir);
        if (hitSelection) {
            if (voxelBrightness > 0.5f) {
                fragColor/=2;
            } else {
                fragColor*=2;
            }
        }
    }

    float tracedDepth = nearClip/dot(mapPos-ogPos, vec3(view[0][2], view[1][2], view[2][2])*-1);
    if (rasterDepth > tracedDepth) {
        fragColor = rasterColor;
    }
}