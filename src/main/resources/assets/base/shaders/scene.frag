int size = 1024;
int height = 320;

uniform mat4 projection;
uniform mat4 view;
uniform ivec3 selected;
uniform bool ui;
uniform ivec2 res;
uniform vec3 sun;
uniform double time;
uniform float timeOfDay;

uniform layout(binding = 0) sampler2D raster_color;
uniform layout(binding = 1) sampler2D raster_depth;
uniform layout(binding = 2) sampler3D atlas;
uniform layout(binding = 3) isampler3D blocks;
uniform layout(binding = 4) sampler3D lights;
uniform layout(binding = 5) sampler2D noises;

layout(std430, binding = 0) buffer playerSSBO
{
    float[] playerData;
};

in vec4 gl_FragCoord;

out vec4 fragColor;

float lerp(float invLerpValue, float toValue, float fromValue) {
    return toValue + invLerpValue * (fromValue - toValue);
}

float inverseLerp(float y, float fromY, float toY) {
    return (y - fromY) / (toY - fromY);
}

float clampedLerp(float toValue, float fromValue, float invLerpValue) {
    if (invLerpValue < 0.0) {
        return toValue;
    } else {
        return invLerpValue > 1.0 ? fromValue : lerp(invLerpValue, toValue, fromValue);
    }
}

float gradient(float y, float fromY, float toY, float fromValue, float toValue) {
    return clampedLerp(toValue, fromValue, inverseLerp(y, fromY, toY));
}

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

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

vec4 shortToColor(int color) {
    return vec4(0xFF & color >> 8, 0xFF & color >> 4, 0xFF & color, 0xFF & color >> 12);
}

bool inBounds(vec3 pos, vec3 bounds) {
    return !(pos.x < 0 || pos.x >= bounds.x || pos.y < 0 || pos.y >= bounds.y || pos.z < 0 || pos.z >= bounds.z);
}

float noise(vec2 coords) {
    return (texture(noises, vec2(coords/1024)).r)-0.5f;
}

vec4 getVoxel(int x, int y, int z, int bX, int bY, int bZ, int blockType, int blockSubtype) {
    return texelFetch(atlas, ivec3(x+(blockType*8), ((abs(y-8)-1)*8)+z, blockSubtype), 0);
}
vec4 getVoxel(float x, float y, float z, float bX, float bY, float bZ, int blockType, int blockSubtype) {
    return getVoxel(int(x), int(y), int(z), int(bX), int(bY), int(bZ), blockType, blockSubtype);
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

bool isCaustic(vec2 checkPos) {
    float samp = noise((checkPos + (float(time) * 100)) * (16+(float(time)/(float(time)/32))))+0.5f;
    if (samp > -0.033 && samp < 0.033) {
        return true;
    }
    return false;
}

bool checker(ivec2 pixel) {
    bool xOdd = bool(pixel.x % 2 == 1);
    bool yOdd = bool(pixel.y % 2 == 1);
    if ((xOdd && yOdd) || (!xOdd && !yOdd)) { //both even or both odd
        return true;
    }
    return false;
}

ivec4 getBlock(float x, float y, float z) {
    return texelFetch(blocks, ivec3(x, y, z), 0);
}
vec4 getLight(float x, float y, float z) {
    return texture(lights, vec3(x, y, z)/vec3(size, height, size), 0)*vec4(4, 4, 4, 10);
}
vec3 sunColor = vec3(0);
vec4 getLightingColor(vec3 lightPos, vec4 lighting, bool isSky) {
    float sunHeight = sun.y/size;
    float scattering = gradient(lightPos.y, 0, 500, 1.5f, -0.5f);
    float sunDist = (distance(lightPos.xz, sun.xz)/(size*1.5f));
    float adjustedTime = clamp((sunDist*abs(1-clamp(sunHeight, 0.05f, 0.5f)))+scattering, 0.f, 1.f);
    float thickness = gradient(lightPos.y, 128, 1500-max(0, sunHeight*1000), 0.33+(sunHeight/2), 1);
    float sunBrightness = clamp(sunHeight+0.5, 0.2f, 1.f);
    float sunSetness = min(1.f, max(abs(sunHeight*1.5f), adjustedTime));
    float whiteness = mix(gradient(lightPos.y, 63, 450, 0, 0.9), 0.9f, clamp(abs(1-sunSetness), 0, 1));
    sunColor = mix(mix(vec3(1, 0.65f, 0.25f)*(1+((10*clamp(sunHeight, 0.f, 0.1f))*(15*min(0.5f, abs(1-sunBrightness))))), vec3(0.36f, 0.54f, 1.2f)*sunBrightness, sunSetness), vec3(sunBrightness), whiteness);
    return vec4(max(lighting.rgb, min(mix(vec3(1), vec3(1, 0.95f, 0.85f), sunSetness/4), lighting.a*sunColor)).rgb, thickness);
}
vec4 powLighting(vec4 lighting) {
    return vec4(pow(lighting.r, 2), pow(lighting.g, 2), pow(lighting.b, 2), pow(lighting.a, 2));
}

bool didntTraceAnything = true;
float voxelBrightness = 0.f;
vec3 ogRayPos = vec3(0);
vec3 hitPos = vec3(0);
vec3 solidHitPos = vec3(0);
vec3 mapPos = vec3(0);
vec3 normal = vec3(0);
vec4 tint = vec4(0);
bool underwater = false;
bool hitCaustic = false;
bool hitSelection = false;

vec4 traceVoxel(vec3 rayPos, vec3 rayDir, float prevRayLength, vec3 iMask, ivec2 block) {
    rayPos *= 8;
    vec3 voxelMapPos = floor(clamp(rayPos, vec3(0.0001f), vec3(7.9999f)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((voxelMapPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;
    vec3 prevVoxelMapPos = voxelMapPos+(stepMask(sideDist+(mask*(-raySign)*deltaDist))*(-raySign));

    for (int i = 0; voxelMapPos.x < 8.0 && voxelMapPos.x >= 0.0 && voxelMapPos.y < 8.0 && voxelMapPos.y >= 0.0 && voxelMapPos.z < 8.0 && voxelMapPos.z >= 0.0 && i < 8*3; i++) {
        vec4 voxelColor = getVoxel(voxelMapPos.x, voxelMapPos.y, voxelMapPos.z, mapPos.x, mapPos.y, mapPos.z, block.x, block.y);

        if (voxelColor.a > 0) {
            vec3 mini = ((voxelMapPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float voxelDist = max(mini.x, max(mini.y, mini.z));
            float rayLength = prevRayLength;
            if (voxelDist > 0.0f) {
                rayLength += voxelDist/8;
            }
            vec3 intersect = rayPos + rayDir*voxelDist;
            vec3 uv3d = intersect - voxelMapPos;

            if (voxelMapPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - voxelMapPos;
            }

            normal = ivec3(voxelMapPos - prevVoxelMapPos);
            solidHitPos = (prevVoxelMapPos/8)+floor(mapPos)+(uv3d/8)-(normal/2);
            if (hitPos == vec3(0)) {
                hitPos = solidHitPos;
                vec3 voxelHitPos = mapPos+(voxelMapPos/8);
                if (ivec2(gl_FragCoord.xy) == ivec2(res/2)) {
                    playerData[0] = voxelHitPos.x;
                    playerData[1] = voxelHitPos.y;
                    playerData[2] = voxelHitPos.z;
                    playerData[3] = (mapPos+(prevVoxelMapPos/8)).x;
                    playerData[4] = (mapPos+(prevVoxelMapPos/8)).y;
                    playerData[5] = (mapPos+(prevVoxelMapPos/8)).z;
                }
                hitSelection = (ivec3(voxelHitPos) == ivec3(playerData[0], playerData[1], playerData[2]));
            }
            if (voxelColor.a < 1) {
                if (block.x == 1) {
                    if (!underwater) {
                        if (isCaustic(vec2(mapPos.x, mapPos.z)+(voxelMapPos.xz/8)+voxelMapPos.y)) {
                            hitCaustic = true;
                            return vec4(fromLinear(vec3(1)), 1);
                        }
                    }
                    tint += voxelColor;
                    underwater = true;
                    return vec4(0);
                }
                tint += voxelColor;
            } else {
                return vec4(voxelColor.rgb, 1);
            }
        }

        mask = stepMask(sideDist);
        prevVoxelMapPos = voxelMapPos;
        voxelMapPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

vec3 lod2Pos = vec3(0);
vec3 lodPos = vec3(0);
ivec4 block = ivec4(0);
vec3 worldSize = vec3(size, height, size);
vec4 traceBlock(vec3 rayPos, vec3 rayDir, float prevRayLength, vec3 iMask) {
    rayPos *= 4;
    vec3 blockPos = floor(clamp(rayPos, vec3(0.0001), vec3(3.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((blockPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;

    for (int i = 0; blockPos.x < 4.0 && blockPos.x >= 0.0 && blockPos.y < 4.0 && blockPos.y >= 0.0 && blockPos.z < 4.0 && blockPos.z >= 0.0 && i < 4*3; i++) {
        mapPos = (lod2Pos*16)+(lodPos*4)+blockPos;
        block = inBounds(mapPos, worldSize) ? getBlock(mapPos.x, mapPos.y, mapPos.z) : (mapPos.y <= 63 ? ivec4(1, 0, 0, 0) : ivec4(0));
        if (block.x > 0 && !(block.x == 1 && underwater)) {
            vec3 uv3d = vec3(0);
            vec3 intersect = vec3(0);
            vec3 mini = ((blockPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float blockDist = max(mini.x, max(mini.y, mini.z));
            intersect = rayPos + rayDir*blockDist;
            uv3d = intersect - blockPos;

            if (blockPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - blockPos;
            }
            vec4 voxelColor = traceVoxel(uv3d, rayDir, prevRayLength+blockDist, mask, block.xy);
            if (voxelColor.a >= 1) {
                return voxelColor;
            }
        }

        mask = stepMask(sideDist);
        blockPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

vec3 lodSize = vec3(size/4, height/4, size/4);
vec4 traceLOD(vec3 rayPos, vec3 rayDir, float prevRayLength, vec3 iMask) {
    rayPos *= 4;
    lodPos = floor(clamp(rayPos, vec3(0.0001), vec3(3.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((lodPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;

    for (int i = 0; lodPos.x < 4.0 && lodPos.x >= 0.0 && lodPos.y < 4.0 && lodPos.y >= 0.0 && lodPos.z < 4.0 && lodPos.z >= 0.0 && i < 4*3; i++) {
        mapPos = (lod2Pos*16)+(lodPos*4);
        int lod = inBounds(lodPos, lodSize) ? texelFetch(blocks, ivec3(lodPos.x, lodPos.y, lodPos.z), 2).x : (lodPos.y <= 4 ? 1 : 0);
        if (lod > 0) {
            vec3 uv3d = vec3(0);
            vec3 intersect = vec3(0);
            vec3 mini = ((lodPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float lodDist = max(mini.x, max(mini.y, mini.z));
            intersect = rayPos + rayDir*lodDist;
            uv3d = intersect - lodPos;

            if (lodPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - lodPos;
            }
            vec4 voxelColor = traceBlock(uv3d, rayDir, prevRayLength+(lodDist*4), mask);
            if (voxelColor.a >= 1) {
                return voxelColor;
            }
        }

        mask = stepMask(sideDist);
        lodPos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

vec3 lod2Size = vec3(size/16, height/16, size/16);
vec4 raytrace(vec3 ogPos, vec3 rayDir) {
    ogRayPos = ogPos;
    vec3 rayPos = ogPos/16;
    lod2Pos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((lod2Pos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    for (int i = 0; i < size/8; i++) { //distance(ogPos, mapPos) < size &&
        mapPos = lod2Pos*16;
        int lod = inBounds(rayPos, lod2Size) ? texelFetch(blocks, ivec3(lod2Pos.x, lod2Pos.y, lod2Pos.z), 4).x : (rayPos.y <= 4 ? 1 : 0);
        if (lod > 0) {
            vec3 uv3d = vec3(0);
            vec3 intersect = vec3(0);
            vec3 mini = ((lod2Pos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float lodDist = max(mini.x, max(mini.y, mini.z));
            intersect = rayPos + rayDir*lodDist;
            uv3d = intersect - lod2Pos;

            if (lod2Pos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - lod2Pos;
            }
            vec4 voxelColor = traceLOD(uv3d, rayDir, lodDist*16, mask);
            if (voxelColor.a >= 1) {
                voxelColor.rgb = fromLinear(voxelColor.rgb)*0.8;
                if (block.x == 31) {
                    voxelColor.rgb *= 1.5f;
                }
                voxelBrightness = max(voxelColor.r, max(voxelColor.g, voxelColor.b));
                if (normal.y >0) { //down
                    voxelColor.rgb *= 0.7f;
                } else if (normal.y <0) { //up
                    voxelColor.rgb *= 1.f;
                } else if (normal.z >0) { //south
                    voxelColor.rgb *= 0.85f;
                } else if (normal.z <0) { //north
                    voxelColor.rgb *= 0.85f;
                } else if (normal.x >0) { //west
                    voxelColor.rgb *= 0.75f;
                } else if (normal.x <0) { //east
                    voxelColor.rgb *= 0.95f;
                }

                didntTraceAnything = false;
                return voxelColor;
            }
        }

        mask = stepMask(sideDist);
        lod2Pos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

float nearClip = 0.1f;

vec3 worldPosFromDepth(float depth) {
    vec2 normalized = gl_FragCoord.xy / res; // [0.5, u_viewPortSize] -> [0, 1]
    vec4 clipSpacePosition = vec4(normalized * 2.0 - 1.0, depth, 1.0); // [0, 1] -> [-1, 1]

    // undo view + projection
    vec4 worldSpacePosition = inverse(projection*view) * clipSpacePosition;
    worldSpacePosition /= worldSpacePosition.w;

    return worldSpacePosition.xyz;
}

bool inArea(int i) {
    return gl_FragCoord.x >= (res.x/2)-i && gl_FragCoord.x < (res.x/2)+i && gl_FragCoord.y >= (res.y/2)-i && gl_FragCoord.y < (res.y/2)+i;
}

void main() {
    vec2 pos = gl_FragCoord.xy;//window space
    vec4 rasterColor = texture(raster_color, pos/res);
    float rasterDepth = texture(raster_depth, pos/res).r;
    //    fragColor = vec4(linearizeDepth(rasterDepth));
    vec2 uv = ((pos / res)*2.f)-1.f;//ndc clip space
    vec4 clipSpace = (inverse(projection) * vec4(uv, -1.f, 1.f));//world space
    clipSpace.w = 0;
    vec3 ogDir = normalize((inverse(view)*clipSpace).xyz);
    vec3 ogPos = inverse(view)[3].xyz;
    bool isSky = rasterColor.a <= 0.f;
    fragColor = raytrace(ogPos, ogDir);
    if (solidHitPos != vec3(0)) {
        isSky = false;
    }
    if (isSky) {
        solidHitPos = mapPos;
    }
    if (hitSelection && ui) {
        fragColor.rgb = mix(fragColor.rgb, vec3(0.7, 0.7, 1), 0.5f);
    }
    vec4 lighting = vec4(-1);
    float tracedDepth = nearClip/dot(solidHitPos-ogPos, vec3(view[0][2], view[1][2], view[2][2])*-1);
    float depth = tracedDepth;
    vec3 lightPos = ogPos + ogDir * size;
    if (rasterDepth > tracedDepth || fragColor.a < 1.f) {
        fragColor = fromLinear(rasterColor);
        depth = rasterDepth;
        solidHitPos = worldPosFromDepth(rasterDepth);
        if (fragColor.a >= 1.f) {
            tint = vec4(0);
            isSky = false;
        }
    }
    if (inBounds(solidHitPos, worldSize)) {
        lighting = fromLinear(getLight(solidHitPos.x, solidHitPos.y, solidHitPos.z));
    } else {
        lighting = fromLinear(vec4(0, 0, 0, 1));
    }
    if (!isSky) {
        lightPos = solidHitPos;
    }
    float fogginess = max(0, sqrt(sqrt(clamp(distance(ogPos, lightPos)/size, 0, 1)))-0.25f)*1.34f;
    lighting.a = mix(lighting.a, fromLinear(vec4(0, 0, 0, 1)).a, fogginess);
    lighting = powLighting(lighting);
    if (fragColor.a < 2) {
        vec4 lightingColor = getLightingColor(lightPos, lighting, isSky);
        fragColor.rgb *= lightingColor.rgb*lighting.a;
        fragColor.rgb = mix(fragColor.rgb, lightingColor.rgb*lighting.a, fogginess);
    }
    if (tint.a > 0) {
        fogginess = max(0, sqrt(sqrt(clamp(distance(ogPos, hitPos)/size, 0, 1)))-0.25f)*1.34f;
        lighting = fromLinear(getLight(hitPos.x, hitPos.y, hitPos.z));
        lighting.a = mix(lighting.a, fromLinear(vec4(0, 0, 0, 1)).a, fogginess);
        lighting = powLighting(lighting);
        vec4 lightingColor = getLightingColor(hitPos, lighting, false);
        vec4 normalizedTint = tint/max(tint.r, max(tint.g, tint.b));
        normalizedTint.rgb *= lightingColor.rgb*lighting.a;
        normalizedTint.rgb = mix(normalizedTint.rgb, lightingColor.rgb*lighting.a, fogginess);
        fragColor.rgb = mix(fragColor.rgb, normalizedTint.rgb, normalizedTint.a);
    }
    fragColor = toLinear(fragColor);
    if (inArea(2)) {
        fragColor = vec4(1);
    }
}