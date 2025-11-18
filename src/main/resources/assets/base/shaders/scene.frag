int size = 1024;
int height = 320;

uniform mat4 projection;
uniform mat4 view;
uniform ivec3 selected;
uniform bool ui;
uniform ivec2 res;
uniform double time;
uniform float timeOfDay;

uniform layout(binding = 0) sampler2D raster_color;
uniform layout(binding = 1) sampler2D raster_depth;
uniform layout(binding = 2, rgba32f) image3D atlas;
uniform layout(binding = 3, rgba16i) iimage3D blocks;

in vec4 gl_FragCoord;

out vec4 fragColor;

// Converts a color from linear light gamma to sRGB gamma
vec4 fromLinear(vec4 linearRGB)
{
    bvec4 cutoff = lessThan(linearRGB, vec4(0.0031308));
    vec4 higher = vec4(1.055)*pow(linearRGB, vec4(1.0/2.4)) - vec4(0.055);
    vec4 lower = linearRGB * vec4(12.92);

    return mix(higher, lower, cutoff);
}

// Converts a color from sRGB gamma to linear light gamma
vec4 toLinear(vec4 sRGB)
{
    bvec4 cutoff = lessThan(sRGB, vec4(0.04045));
    vec4 higher = pow((sRGB + vec4(0.055))/vec4(1.055), vec4(2.4));
    vec4 lower = sRGB/vec4(12.92);

    return mix(higher, lower, cutoff);
}

vec3 fromLinear(vec3 linearRGB)
{
    bvec3 cutoff = lessThan(linearRGB, vec3(0.0031308));
    vec3 higher = vec3(1.055)*pow(linearRGB, vec3(1.0/2.4)) - vec3(0.055);
    vec3 lower = linearRGB * vec3(12.92);

    return vec3(mix(higher, lower, cutoff));
}

// Converts a color from sRGB gamma to linear light gamma
vec3 toLinear(vec3 sRGB)
{
    bvec3 cutoff = lessThan(sRGB, vec3(0.04045));
    vec3 higher = pow((sRGB + vec3(0.055))/vec3(1.055), vec3(2.4));
    vec3 lower = sRGB/vec3(12.92);

    return vec3(mix(higher, lower, cutoff));
}

float noise(vec2 coords) {
    return 0.5f;
}

bool hitSelection = false;
vec4 getVoxel(int x, int y, int z, int bX, int bY, int bZ, int blockType, int blockSubtype, float fire) {
    vec4 color = imageLoad(atlas, ivec3(x+(blockType*8), ((abs(y-8)-1)*8)+z, blockSubtype)) + (fire > 0 ? (vec4(vec3(1, 0.3, 0.05)*(abs(max(0, noise((vec2(x+bX, y+bZ)*64)+(float(time)*10000))+noise((vec2(y+bX, z+bZ)*8)+(float(time)*10000))+noise((vec2(z+bZ+x+bX, x+bY)*64)+(float(time)*10000)))*6.66)*fire), 0)) : vec4(0));

    color.rgb = fromLinear(color.rgb)*0.8;
    if (ui && selected == ivec3(bX, bY, bZ) && color.a > 0) {
        hitSelection = true;
    }
    if (blockType == 31) {
        color.rgb *= 1.5f;
    }
    return color;

}
vec4 getVoxel(float x, float y, float z, float bX, float bY, float bZ, int blockType, int blockSubtype, float fire) {
    return getVoxel(int(x), int(y), int(z), int(bX), int(bY), int(bZ), blockType, blockSubtype, fire);
}

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
        vec4 voxelColor = getVoxel(4, 4, 4, mapPos.x, mapPos.y, mapPos.z, block.x, block.y, 0.f);
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