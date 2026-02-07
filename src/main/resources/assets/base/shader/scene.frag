int size = 1024;
int height = 320;

uniform mat4 projection;
uniform mat4 view;
uniform ivec3 selected;
uniform bool ui;
uniform ivec2 res;
uniform vec3 sun;
uniform vec3 mun;
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

bool castsFullShadow(ivec4 block) {
    return block.x != 4 && block.x != 5 && block.x != 14 && block.x != 18 && block.x != 30 && block.x != 52 && block.x != 53 && block.x != 17;
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
    return texelFetch(blocks, ivec3(z, y, x), 0);
}
vec4 getLight(float x, float y, float z) {
    return texture(lights, vec3(z, y, x)/vec3(size, height, size), 0)*vec4(7.5f, 7.5f, 7.5f, 10);
}
vec3 sunColor = vec3(0);
vec4 getLightingColor(vec3 lightPos, vec4 lighting, bool isSky, float fogginess) {
    float sunHeight = sun.y/size;
    float scattering = gradient(lightPos.y, 0, 500, 1.5f, -0.5f);
    float sunDist = (distance(lightPos.xz, sun.xz)/(size*1.5f));
    float adjustedTime = clamp((sunDist*abs(1-clamp(sunHeight, 0.05f, 0.5f)))+scattering, 0.f, 1.f);
    float thickness = gradient(lightPos.y, 128, 1500-max(0, sunHeight*1000), 0.33+(sunHeight/2), 1);
    float sunBrightness = clamp(sunHeight+0.5, 0.33f, 1.f);
    float sunSetness = min(1.f, max(abs(sunHeight*1.5f), adjustedTime));
    float skyWhiteness = mix(gradient(lightPos.y, 63, 450, 0, 0.9), 0.9f, clamp(abs(1-sunSetness), 0, 1));
    float whiteness = isSky ? skyWhiteness : mix(0.9f, skyWhiteness, max(0, fogginess-0.8f)*5.f);
    sunColor = mix(mix(vec3(1, 0.65f, 0.25f)*(1+((10*clamp(sunHeight, 0.f, 0.1f))*(15*min(0.5f, abs(1-sunBrightness))))), vec3(0.36f, 0.54f, 1.2f)*sunBrightness, sunSetness), vec3(sunBrightness), whiteness);
    return vec4(max(lighting.rgb, min(mix(vec3(1), vec3(1, 0.95f, 0.85f), sunSetness/4), lighting.a*sunColor)).rgb, thickness);
}
vec4 powLighting(vec4 lighting) {
    return vec4(lighting.r, lighting.g, lighting.b, pow(lighting.a, 2));
}

bool isFirstRay = true;
vec3 hitPos = vec3(0);
vec3 solidHitPos = vec3(0);
vec3 mapPos = vec3(0);
vec3 normal = vec3(0);
vec4 tint = vec4(0);
bool underwater = false;
bool hitCaustic = false;
bool hitSelection = false;
bool isInfiniteSea = false;
bool isShadow = false;
float shade = 0.f;

vec3 ogRayPos = vec3(0);
vec3 prevPos = vec3(0);
vec3 lod2Pos = vec3(0);
vec3 lodPos = vec3(0);
ivec4 block = ivec4(0);
vec3 worldSize = vec3(size, height, size);

void clearVars() {
    shade = 0.f;
    hitPos = vec3(0);
    solidHitPos = vec3(0);
    mapPos = vec3(0);
    normal = vec3(0);
    underwater = false;
    hitCaustic = false;
    hitSelection = false;
    isInfiniteSea = false;
    tint = vec4(0);
    prevPos = vec3(0);
    lod2Pos = vec3(0);
    lodPos = vec3(0);
    block = ivec4(0);
}
bool isBlockLeaves(ivec2 block) {
    return block.y == 0 && (block.x == 17 || block.x == 21 || block.x == 27 || block.x == 36 || block.x == 39 || block.x == 42 || block.x == 45 || block.x == 48 || block.x == 51);
}
float one = fromLinear(vec4(1)).a;
vec4 getVoxelAndBlock(vec3 pos) {
    vec3 rayMapPos = floor(pos);
    vec3 mapPos = (pos-rayMapPos)*8;
    ivec2 block = getBlock(rayMapPos.x, rayMapPos.y, rayMapPos.z).xy;
    if (block.x <= 1) {
        return vec4(0.f);
    } else if (isBlockLeaves(block)) {
        return vec4(one);
    }
    return getVoxel(mapPos.x, mapPos.y, mapPos.z, rayMapPos.x, rayMapPos.y, rayMapPos.z, block.x, block.y);
}

vec4 traceBlock(vec3 rayPos, vec3 rayDir, vec3 iMask, float subChunkDist, float chunkDist) {
    rayPos *= 4;
    vec3 blockPos = floor(clamp(rayPos, vec3(0.0001), vec3(3.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((blockPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;

    vec3 mini = ((blockPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
    float blockDist = max(mini.x, max(mini.y, mini.z));

    vec3 voxelRayPos = vec3(0.f);
    vec3 voxelPos = vec3(0.f);
    vec3 voxelSideDist = sideDist;
    vec3 voxelMask = mask;
    vec3 prevVoxelPos = vec3(0);

    bool steppingBlock = true;

    for (int i = 0; blockPos.x < 4.0 && blockPos.x >= 0.0 && blockPos.y < 4.0 && blockPos.y >= 0.0 && blockPos.z < 4.0 && blockPos.z >= 0.0 && i < (4*8)*3; i++) {
        if (steppingBlock) {
            mapPos = (lod2Pos*16)+(lodPos*4)+blockPos;
            block = isInfiniteSea ? ivec4(1, 15, 0, 0) : (inBounds(mapPos, worldSize) ? getBlock(mapPos.x, mapPos.y, mapPos.z) : ivec4(0));
            if (block.x > 0 && !(block.x == 1 && underwater)) {
                steppingBlock = false;
                mini = ((blockPos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float blockDist = max(mini.x, max(mini.y, mini.z));
                vec3 intersect = rayPos + rayDir*blockDist;
                voxelRayPos = intersect - blockPos;
                if (blockPos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                    voxelRayPos = rayPos - blockPos;
                }
                voxelRayPos *= 8;

                voxelPos = floor(clamp(voxelRayPos, vec3(0.0001f), vec3(7.9999f)));
                voxelSideDist = ((voxelPos - voxelRayPos) + 0.5 + raySign * 0.5) * deltaDist;
                voxelMask = mask;
                prevVoxelPos = voxelPos+(stepMask(voxelSideDist+(voxelMask*(-raySign)*deltaDist))*(-raySign));

                float rayLength = 0.f;
                vec3 voxelMini = ((voxelPos-voxelRayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float voxelDist = max(voxelMini.x, max(voxelMini.y, voxelMini.z));
                if (voxelDist > 0.0f) {
                    rayLength += voxelDist/8;
                }
                if (blockDist > 0.0f) {
                    rayLength += blockDist;
                }
                if (subChunkDist > 0.0f) {
                    rayLength += (subChunkDist*4);
                }
                if (chunkDist > 0.0f) {
                    rayLength += (chunkDist*16);
                }
                vec3 hitNormal = -voxelMask * raySign;
                vec3 realPos = (ogRayPos + rayDir * rayLength);
                prevPos = realPos + (hitNormal * 0.001f);
            }
        } else if (voxelPos.x < 8.0 && voxelPos.x >= 0.0 && voxelPos.y < 8.0 && voxelPos.y >= 0.0 && voxelPos.z < 8.0 && voxelPos.z >= 0.0) {
            vec3 offsetVoxelPos = voxelPos;
            if (block.x == 4 && offsetVoxelPos.y > 2.0) {
                bool windDir = timeOfDay > 0.f;
                float windStr = noise(((vec2(mapPos.x, mapPos.z)/48) + (float(time) * 100)) * (16+(float(time)/(float(time)/32))))+0.5f;
                if (windStr > 0.8) {
                    offsetVoxelPos.x = offsetVoxelPos.x+((offsetVoxelPos.y > 5 ? 3 : (offsetVoxelPos.y > 4 ? 2 : 1)) * (windDir ? -1 : 1));
                    if (block.y < 2) {
                        offsetVoxelPos.z = offsetVoxelPos.z+(offsetVoxelPos.y > 4 ? 2 : 1);
                    }
                } else if (windStr > 0.4) {
                    offsetVoxelPos.x = offsetVoxelPos.x+((offsetVoxelPos.y > 5 ? 3 : (offsetVoxelPos.y > 4 ? 2 : 1)) * (windDir ? -1 : 1));
                    if (block.y < 2) {
                        offsetVoxelPos.z = offsetVoxelPos.z+(offsetVoxelPos.y > 4 ? 1 : 0);
                    }
                } else if (windStr > -0.2) {
                    offsetVoxelPos.x = offsetVoxelPos.x+((offsetVoxelPos.y > 4 ? 2 : 1) * (windDir ? -1 : 1));
                    if (block.y < 2) {
                        offsetVoxelPos.z = offsetVoxelPos.z+(offsetVoxelPos.y > 4 ? 1 : 0);
                    }
                } else if (windStr > -0.8) {
                    offsetVoxelPos.x = offsetVoxelPos.x+((offsetVoxelPos.y > 4 ? 1 : 0) * (windDir ? -1 : 1));
                }
                offsetVoxelPos.xz = clamp(offsetVoxelPos.xz, 0, 7);
            }
            vec4 voxelColor = getVoxel(offsetVoxelPos.x, offsetVoxelPos.y, offsetVoxelPos.z, mapPos.x, mapPos.y, mapPos.z, block.x, block.y);
            if (voxelColor.a > 0) {
                vec3 voxelMini = ((voxelPos-voxelRayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float voxelDist = max(voxelMini.x, max(voxelMini.y, voxelMini.z));
                vec3 intersect = voxelRayPos + rayDir*voxelDist;
                vec3 uv3d = intersect - voxelPos;

                if (voxelPos == floor(voxelRayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = voxelRayPos - voxelPos;
                }

                normal = ivec3(voxelPos - prevVoxelPos);
                solidHitPos = (prevVoxelPos/8)+floor(mapPos)+(uv3d/8)-(normal/2);
                if (hitPos == vec3(0)) {
                    hitPos = solidHitPos;
                }
                if (voxelColor.a < 1) {
                    if (block.x == 1) {
                        bool topVoxel = voxelPos.y >= 7;
                        if (!underwater && (isInfiniteSea || getVoxel(voxelPos.x, topVoxel ? 0 : voxelPos.y+1, voxelPos.z, mapPos.x, mapPos.y + (topVoxel ? 1 : 0), mapPos.z, block.x, block.y).a <= 0)) {
                            if (isCaustic(vec2(mapPos.x, mapPos.z)+(voxelPos.xz/8)+voxelPos.y)) {
                                hitCaustic = true;
                                return vec4((isInfiniteSea ? 2 : 1) * fromLinear(vec3(1)), 1);
                            }
                        }
                        underwater = true;
                        steppingBlock = true;
                    }
                    tint += voxelColor;
                } else {
                    shade += 0.1f;
                    if (shade > 0.33f) {
                        shade = 0.33f;
                    }
                    vec3 voxelHitPos = mapPos+(voxelPos/8);
                    if (isFirstRay) {
                        if (ivec2(gl_FragCoord.xy) == ivec2(res/2)) {
                            playerData[0] = voxelHitPos.x;
                            playerData[1] = voxelHitPos.y;
                            playerData[2] = voxelHitPos.z;
                            playerData[3] = (mapPos+(prevVoxelPos/8)).x;
                            playerData[4] = (mapPos+(prevVoxelPos/8)).y;
                            playerData[5] = (mapPos+(prevVoxelPos/8)).z;
                        }
                    }
                    hitSelection = (ivec3(voxelHitPos) == ivec3(playerData[0], playerData[1], playerData[2]));
                    if (!isShadow) {
                        float xFactor = offsetVoxelPos.x >= 4 ? 0.125f : -0.125f;
                        float yFactor = offsetVoxelPos.y >= 4 ? 0.125f : -0.125f;
                        float zFactor = offsetVoxelPos.z >= 4 ? 0.125f : -0.125f;
                        float highlight = 0.67f;
                        if (getVoxelAndBlock(mapPos+(offsetVoxelPos/8)+vec3(xFactor, 0, 0)).a < one) {
                            highlight+=0.33f;
                        }
                        if (getVoxelAndBlock(mapPos+(offsetVoxelPos/8)+vec3(0, 0, zFactor)).a < one) {
                            highlight+=0.33f;
                        }
                        if (getVoxelAndBlock(mapPos+(offsetVoxelPos/8)+vec3(0, yFactor, 0)).a < one) {
                            highlight+=0.33f;
                        }
                        voxelColor.rgb *= min(highlight, 1.33f);
                    }
                    if (!isShadow || shade >= 0.33f || castsFullShadow(block)) {
                        return vec4(voxelColor.rgb, 1);
                    }
                }
                if (isInfiniteSea) {
                    return vec4(-1);
                }
            }
            if (!steppingBlock) {
                voxelMask = stepMask(voxelSideDist);
                prevVoxelPos = voxelPos;
                voxelPos += voxelMask * raySign;
                voxelSideDist += voxelMask * raySign * deltaDist;
                float rayLength = 0.f;
                vec3 voxelMini = ((voxelPos-voxelRayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
                float voxelDist = max(voxelMini.x, max(voxelMini.y, voxelMini.z));
                if (voxelDist > 0.0f) {
                    rayLength += voxelDist/8;
                }
                if (blockDist > 0.0f) {
                    rayLength += blockDist;
                }
                if (subChunkDist > 0.0f) {
                    rayLength += (subChunkDist*4);
                }
                if (chunkDist > 0.0f) {
                    rayLength += (chunkDist*16);
                }
                vec3 hitNormal = -voxelMask * raySign;
                vec3 realPos = (ogRayPos + rayDir * rayLength);
                prevPos = realPos + (hitNormal * 0.001f);
            }
        } else {
            steppingBlock = true;
        }
        if (steppingBlock) {
            mask = stepMask(sideDist);
            blockPos += mask * raySign;
            sideDist += mask * raySign * deltaDist;
            mini = ((blockPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
            blockDist = max(mini.x, max(mini.y, mini.z));
        }
    }

    return vec4(0);
}

vec3 lodSize = vec3(size/4, height/4, size/4);
vec4 traceLOD(vec3 rayPos, vec3 rayDir, vec3 iMask, float chunkDist) {
    rayPos *= 4;
    lodPos = floor(clamp(rayPos, vec3(0.0001), vec3(3.9999)));
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((lodPos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = iMask;

    for (int i = 0; lodPos.x < 4.0 && lodPos.x >= 0.0 && lodPos.y < 4.0 && lodPos.y >= 0.0 && lodPos.z < 4.0 && lodPos.z >= 0.0 && i < 4*3; i++) {
        mapPos = (lod2Pos*16)+(lodPos*4);
        int lod = isInfiniteSea ? 1 : texelFetch(blocks, ivec3(lodPos.z, lodPos.y, lodPos.x), 2).x;
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
            vec4 voxelColor = traceBlock(uv3d, rayDir, mask, lodDist, chunkDist);
            if (voxelColor.a >= 1 || voxelColor.a <= -1) {
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
    if (rayDir.x == 0.0f) {
        rayDir.x = 0.001f;
    }
    if (rayDir.y == 0.0f) {
        rayDir.y = 0.001f;
    }
    if (rayDir.z == 0.0f) {
        rayDir.z = 0.001f;
    }
    vec3 rayPos = ogPos/16;
    lod2Pos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((lod2Pos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    for (int i = 0; distance(rayPos, lod2Pos) < size/16 && i < size/8; i++) {
        mapPos = lod2Pos*16;
        bool inBound = inBounds(lod2Pos, lod2Size);
        if (!inBound && rayDir.y >= 0.f) {
            break;
        }
        isInfiniteSea = !inBound && (lod2Pos.y == 3) && ogPos.y > 63;
        int lod = isInfiniteSea ? ((mapPos.y < 64) ? 1 : 0) : (inBound ? texelFetch(blocks, ivec3(lod2Pos.z, lod2Pos.y, lod2Pos.x), 4).x : 0);
        if (lod > 0) {
            vec3 uv3d = vec3(0);
            vec3 intersect = vec3(0);
            vec3 mini = ((lod2Pos-rayPos) + 0.5 - 0.5*vec3(raySign))*deltaDist;
            float lod2Dist = max(mini.x, max(mini.y, mini.z));
            intersect = rayPos + rayDir*lod2Dist;
            uv3d = intersect - lod2Pos;

            if (lod2Pos == floor(rayPos)) { // Handle edge case where camera origin is inside of block
                uv3d = rayPos - lod2Pos;
            }
            vec4 voxelColor = traceLOD(uv3d, rayDir, mask, lod2Dist);
            if (voxelColor.a >= 1 || voxelColor.a <= -1) {
                voxelColor.rgb = fromLinear(voxelColor.rgb)*0.8;

                return voxelColor;
            }
        }

        mask = stepMask(sideDist);
        lod2Pos += mask * raySign;
        sideDist += mask * raySign * deltaDist;
    }

    return vec4(0);
}

float nearClip = 0.01f;

vec3 worldPosFromDepth(float depth) {
    vec2 coords = gl_FragCoord.xy / res;
    vec4 clipSpacePos = vec4((coords * 2.0) - 1.0, depth, 1.0);
    vec4 viewSpacePos = inverse(projection)*clipSpacePos;
    viewSpacePos /= viewSpacePos.w;
    return (inverse(view)*viewSpacePos).xyz;
}

void main() {
    mat4 invView = inverse(view);
    vec2 pos = gl_FragCoord.xy;
    vec4 camClipSpace = vec4((inverse(projection) * vec4(0, 0, 1.f, 1.f)).xyz, 0);
    vec3 camDir = normalize((invView*camClipSpace).xyz);
    vec2 uv = ((pos / res)*2.f)-1.f;
    vec4 clipSpace = vec4((inverse(projection) * vec4(uv, 1.f, 1.f)).xyz, 0);
    vec3 ogDir = normalize((invView*clipSpace).xyz);
    vec3 ogPos = invView[3].xyz;
    vec4 rasterColor = texture(raster_color, pos/res);
    float rasterDepth = texture(raster_depth, pos/res).r;
    bool isSky = rasterColor.a <= 0.f;
    fragColor = raytrace(ogPos, ogDir);
    isFirstRay = false;
    if (solidHitPos != vec3(0)) {
        isSky = false;
    }
    if (fragColor.a < 1) {
        isSky = true;
    }
    if (isSky) {
        solidHitPos = mapPos;
    }
    if (hitSelection && ui) {
        fragColor.rgb = mix(fragColor.rgb, vec3(0.7, 0.7, 1), 0.5f);
    }
    vec4 lighting = vec4(-1);
    vec3 lightPos = ogPos + ogDir * size;
    float tracedDepth = nearClip/max(0, dot((solidHitPos+(normal/2))-ogPos, camDir));
    float depth = tracedDepth;
    //fragColor = (checker(ivec2(pos/32) ? vec4(tracedDepth) : vec4(rasterDepth)))*4;
    if (rasterDepth > tracedDepth || fragColor.a < 1.f) {
        vec3 rasterPos = ivec3(worldPosFromDepth(rasterDepth)*8.f)/8.f;
        if (rasterPos.y > 63 || (rasterPos.y < height && rasterPos.x > 0 && rasterPos.x < size && rasterPos.z > 0 && rasterPos.z < size)) { //if out of bounds, only render when above sea level.
            depth = rasterDepth;
            fragColor.rgb = fromLinear(rasterColor).rgb;
            fragColor.a = rasterColor.a;
            normal = vec3(1);
            prevPos = ivec3(worldPosFromDepth(rasterDepth)*8.f)/8.f;
            solidHitPos = rasterPos;
            if (fragColor.a > 0) {
                tint = vec4(0);
                isSky = false;
            }
        }
    }
    if (inBounds(solidHitPos, worldSize)) {
        lighting = fromLinear(getLight(solidHitPos.x, solidHitPos.y, solidHitPos.z));
    } else {
        lighting = fromLinear(vec4(0, 0, 0, 1));
    }
    float shadowFactor = 1.f;
    if (!isSky) {
        lightPos = solidHitPos;
        if (fragColor.a < 2) {
            vec3 source = mun.y > sun.y ? mun : sun;
            source.y = max(source.y, 500);
            vec3 shadowPos = mix((floor(prevPos*8)+0.5f)/8, prevPos, abs(normal));
            float brightness = dot(normal.xy, source.xy)*-0.0002f;
            fragColor.rgb *= clamp(0.75f+brightness, 0.66f, 1.f);
            vec3 sunDir = vec3(normalize(source.xy - (worldSize.xy/2)), 0.1f);
            vec4 prevTint = tint;
            vec3 prevHitPos = hitPos;
            clearVars();
            isShadow = true;
            bool solidCaster = raytrace(shadowPos, sunDir).a > 0.0f;
            if (shade > 0.f) {
                shadowFactor *= solidCaster ? min(0.9f, mix(0.66f, 0.9f, min(1, distance(shadowPos, hitPos)/420))) : 1-shade;
            }
            isShadow = false;
            tint = prevTint;
            hitPos = prevHitPos;
        } else if (fragColor.a >= 10) {
            fragColor.a -= 10;
            shadowFactor = 0.66f;
        }
    }
    float fogginess = clamp((sqrt(sqrt(clamp(distance(ogPos, lightPos)/size, 0, 1)))-0.25f)*1.34f, 0.f, 1.f);
    lighting.a = mix(lighting.a*shadowFactor, fromLinear(vec4(0, 0, 0, 1)).a, fogginess);
    lighting = powLighting(lighting);
    if (fragColor.a < 2) {
        vec4 lightingColor = getLightingColor(lightPos, lighting, isSky, fogginess);
        fragColor.rgb *= lightingColor.rgb;
        fragColor.rgb = mix(fragColor.rgb*1.2f, lightingColor.rgb, fogginess);
    }
    if (tint.a > 0) {
        fogginess = clamp((sqrt(sqrt(clamp(distance(ogPos, hitPos)/size, 0, 1)))-0.25f)*1.34f, 0.f, 1.f);
        lighting = fromLinear(getLight(hitPos.x, hitPos.y, hitPos.z));
        lighting.a = mix(lighting.a*shadowFactor, fromLinear(vec4(0, 0, 0, 1)).a, fogginess);
        lighting = powLighting(lighting);
        vec4 lightingColor = getLightingColor(hitPos, lighting, isSky, fogginess);
        vec4 normalizedTint = tint/max(tint.r, max(tint.g, tint.b));
        normalizedTint.rgb *= lightingColor.rgb*lighting.a;
        normalizedTint.rgb = mix(normalizedTint.rgb, lightingColor.rgb*lighting.a, pow(fogginess, 2));
        fragColor.rgb = mix(fragColor.rgb, normalizedTint.rgb, normalizedTint.a*0.75f);
    }
    fragColor = toLinear(fragColor);
    fragColor.a = depth;
}