uniform mat4 projection;
uniform mat4 view;
uniform ivec3 selected;
uniform bool taa;
uniform bool ui;
uniform bool upscale;
uniform bool shadowsEnabled;
uniform ivec2 res;
uniform vec3 sun;
uniform vec3 mun;
uniform double time;
uniform float timeOfDay;
uniform int offsetIdx;

uniform layout(binding = 0) sampler2D raster_color;
uniform layout(binding = 1) sampler2D raster_pos;
uniform layout(binding = 2) sampler2D raster_norm;
uniform layout(binding = 3) sampler3D atlas;
uniform layout(binding = 4) isampler3D blocks;
uniform layout(binding = 5) sampler3D lights;
uniform layout(binding = 6) sampler2D noises;

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

const int size = 1024;
const int height = 320;
const vec3 worldSize = vec3(size, height, size);
const float alphaMax = 0.95f;
const float one = fromLinear(vec4(1)).a;
const float eigth = 1f/8f;

bool inBounds(vec3 pos, vec3 bounds) {
    return !(pos.x < 0 || pos.x >= bounds.x || pos.y < 0 || pos.y >= bounds.y || pos.z < 0 || pos.z >= bounds.z);
}

float noise(vec2 coords) {
    return (texture(noises, vec2(coords/1024)).r)-0.5f;
}
float whiteNoise(vec2 coords) {
    return (texture(noises, vec2(coords/1024)).g)-0.5f;
}

bool castsFullShadow(ivec4 block) {
    return block.x != 4 && block.x != 5 && block.x != 14 && block.x != 18 && block.x != 30 && block.x != 52 && block.x != 53;
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

float getCaustic(vec2 checkPos) {
    return noise((checkPos + (float(time) * 100)) * (16+(float(time)/(float(time)/32))))+0.5f;
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
float waterDepth = 1.f;
vec4 getLight(float x, float y, float z) {
    return texture(lights, vec3(z, y, x)/vec3(size, height, size), 0)*vec4(7.5f, 7.5f, 7.5f, 10);
}
vec3 ogPos = vec3(0);
vec3 sunColor = vec3(0);
float fogDetractorFactor = 1.f;
vec4 getLightingColor(vec3 lightPos, vec4 lighting, bool isSky, float fogginess) {
    float ogY = ogPos.y;
    float sunHeight = sun.y/size;
    float scattering = gradient(lightPos.y, ogY-63, ogY+437, 1.5f, -0.5f);
    float sunDist = (distance(lightPos.xz, sun.xz)/(size*1.5f));
    float adjustedTime = clamp((sunDist*abs(1-clamp(sunHeight, 0.05f, 0.5f)))+scattering, 0.f, 1.f);
    float thickness = gradient(lightPos.y, 128, 1500-max(0, sunHeight*1000), 0.33+(sunHeight/2), 1);
    float sunBrightness = clamp(sunHeight+0.5, 0.33f, 1.f);
    if (fogDetractorFactor == -1) {
        fogDetractorFactor = max(max(lighting.r, max(lighting.g, lighting.b))*0.8f, (sunBrightness*lighting.a)*1.5f);
    }
    float sunSetness = min(1.f, max(abs(sunHeight*1.5f), adjustedTime));
    float whiteY = max(ogY, 200)-135.f;
    float skyWhiteness = mix(max(0.33f, gradient(lightPos.y, (whiteY/4)+47, (whiteY/2)+436, 0, 0.9)), 0.9f, clamp(abs(1-sunSetness), 0, 1.f));
    float whiteness = isSky ? skyWhiteness : mix(0.9f, skyWhiteness, max(0, fogginess-0.8f)*5.f);
    sunColor = mix(mix(vec3(1, 0.65f, 0.25f)*(1+((10*clamp(sunHeight, 0.f, 0.1f))*(15*min(0.5f, abs(1-sunBrightness))))), vec3(0.36f, 0.54f, 1.2f)*sunBrightness, sunSetness), vec3(sunBrightness), whiteness);
    return vec4(max(lighting.rgb, min(fromLinear(mix(vec3(1), vec3(1, 0.95f, 0.85f), sunSetness/4)), lighting.a*sunColor)).rgb, thickness);
}
vec4 powLighting(vec4 lighting) {
    return vec4(lighting.r, lighting.g, lighting.b, pow(lighting.a, 2));
}

bool shouldSelectBlock = true;
ivec3 hitBlock = ivec3(0);
vec3 hitPos = vec3(0);
vec3 solidHitPos = vec3(0);
vec3 mapPos = vec3(0);
vec3 normal = vec3(0);
vec4 tint = vec4(0);
bool underwater = false;
bool hitCaustic = false;
bool isInfiniteSea = false;
bool isShadow = false;

vec3 ogRayPos = vec3(0);
vec3 prevPos = vec3(0);
vec3 lod2Pos = vec3(0);
vec3 lodPos = vec3(0);
ivec4 block = ivec4(0);
vec4 texColor = vec4(0);
vec4 lightFog = vec4(0);
bool hitSolidVoxel = false;
float maxRayDist = 2500.f;
vec4 prevTintAddition = vec4(0);

void clearVars() {
    prevTintAddition = vec4(0);
    maxRayDist = 2500.f;
    hitBlock = ivec3(0);
    hitPos = vec3(0);
    solidHitPos = vec3(0);
    mapPos = vec3(0);
    normal = vec3(0);
    underwater = false;
    hitCaustic = false;
    isInfiniteSea = false;
    tint = vec4(0);
    prevPos = vec3(0);
    lod2Pos = vec3(0);
    lodPos = vec3(0);
    block = ivec4(0);
    texColor = vec4(0);
    hitSolidVoxel = false;
}
vec3 source = vec3(0);
bool isBlockLeaves(ivec2 block) {
    return block.y == 0 && (block.x == 17 || block.x == 21 || block.x == 27 || block.x == 36 || block.x == 39 || block.x == 42 || block.x == 45 || block.x == 48 || block.x == 51);
}
bool isLightSource(ivec2 block) {
    return block.x == 6 || block.x == 7 || block.x == 14 || block.x == 19 || block.x == 52 || block.x == 53;
}
bool isFullSemitransparentBlock(ivec2 block) {
    return block.x == 11 || block.x == 12 || block.x == 13;
}
bool isGlassSolid(vec3 mapPos, vec3 rayMapPos) {
    float samp = whiteNoise(((vec2(mapPos.x, mapPos.z)*128)+(rayMapPos.y*8)+mapPos.y)+(vec2(rayMapPos.x, rayMapPos.z)*8));
    if (samp > -0.004 && samp < -0.002 || samp > 0 && samp < 0.002) {
        return true;
    }
    return false;
}
vec4 lightFogLastCheck = vec4(0);
void updateLightFog(vec3 pos) {
    if (!isShadow) {
        lightFogLastCheck = fromLinear(getLight(pos.x, pos.y, pos.z));
        lightFog = max(lightFog, getLightingColor(mapPos, vec4(lightFogLastCheck.rgb, 0), false, 1)/2);
    }
}

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
vec4 getVoxelAndBlockWOLeavesOverride(vec3 pos) {
    vec3 rayMapPos = floor(pos);
    vec3 mapPos = (pos-rayMapPos)*8;
    ivec2 block = getBlock(rayMapPos.x, rayMapPos.y, rayMapPos.z).xy;
    if (block.x <= 1) {
        return vec4(0.f);
    }
    return getVoxel(mapPos.x, mapPos.y, mapPos.z, rayMapPos.x, rayMapPos.y, rayMapPos.z, block.x, block.y);
}

vec3 ogDir = vec3(0);
vec3 rayDir = vec3(0);
vec3 roundDir(vec3 dir) {
    if (dir.x == 0.0f) {
        dir.x = 0.001f;
    }
    if (dir.y == 0.0f) {
        dir.y = 0.001f;
    }
    if (dir.z == 0.0f) {
        dir.z = 0.001f;
    }
    return dir;
}
vec3 tintNormal = vec3(0);
vec4 traceBlock(vec3 rayPos, vec3 iMask, float subChunkDist, float chunkDist) {
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
    bool firstStep = true;
    for (int i = 0; blockPos.x < 4.0 && blockPos.x >= 0.0 && blockPos.y < 4.0 && blockPos.y >= 0.0 && blockPos.z < 4.0 && blockPos.z >= 0.0 && i < (4*8)*3; i++) {
        vec4 tintAddition = vec4(0.f);
        if (steppingBlock) {
            if (firstStep) {
                firstStep = false;
                mapPos = (lod2Pos*16)+(lodPos*4)+blockPos;
                block = inBounds(mapPos, worldSize) ? getBlock(mapPos.x, mapPos.y, mapPos.z) : (mapPos.y > 63 ? ivec4(0) : (mapPos.y == 63 ? ivec4(1, 14, 0, 0) : ivec4(0)));
            }
            updateLightFog(mapPos+0.5f);
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
            if (distance(ogPos, mapPos+(voxelPos/8)) >= maxRayDist) {
                return vec4(-1);
            }
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
            vec4 baseColor = getVoxel(offsetVoxelPos.x, offsetVoxelPos.y, offsetVoxelPos.z, mapPos.x, mapPos.y, mapPos.z, block.x, block.y);
            vec4 voxelColor = baseColor;
            if (voxelColor.a > 0) {
                vec3 voxelHitPos = mapPos+(voxelPos/8);
                if (shouldSelectBlock && block.x > 1) {
                    if (ivec2(gl_FragCoord.xy) == (upscale ? ivec2(res/vec2(4, 2)) : ivec2(res/2))) {
                        shouldSelectBlock = false;
                        playerData[0] = voxelHitPos.x;
                        playerData[1] = voxelHitPos.y;
                        playerData[2] = voxelHitPos.z;
                        playerData[3] = (mapPos+(prevVoxelPos/8)).x;
                        playerData[4] = (mapPos+(prevVoxelPos/8)).y;
                        playerData[5] = (mapPos+(prevVoxelPos/8)).z;
                    }
                }
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
                if (hitBlock == ivec3(0) && block.x > 1) { //dont detect water
                    hitBlock = ivec3(mapPos);
                }
                if (voxelColor.a < alphaMax) {
                    if (tintNormal == vec3(0)) {
                        tintNormal = normal;
                    }
                    if (block.x == 1) {
                        voxelColor = mix(voxelColor, mix(vec4(1.f, 0.6f, 0.f, 0.95f), voxelColor, clamp(distance(sun.y, 64)/256, 0.f, 1.f)), lightFogLastCheck.a);
                        bool topVoxel = voxelPos.y >= 7; //-(block.y/16)
                        if (!underwater && (isInfiniteSea || getVoxel(voxelPos.x, topVoxel ? 0 : voxelPos.y+1, voxelPos.z, mapPos.x, mapPos.y + (topVoxel ? 1 : 0), mapPos.z, block.x, block.y).a <= 0)) {
                            float causticness = getCaustic(vec2(mapPos.x, mapPos.z)+(voxelPos.xz/8)+voxelPos.y);
                            if (causticness > -0.033 && causticness < 0.033) {
                                hitCaustic = true;
                                return vec4(fromLinear(vec3(1)), 1);
                            } else {
                                voxelColor.rgb = mix(voxelColor.rgb, vec3(1), abs(1-abs(causticness))/3);
                            }
                        }
                        underwater = true;
                        steppingBlock = true;
                    }
                    float tintMul = 1.f;
                    if (isFullSemitransparentBlock(block.xy)) {
                        steppingBlock = true;
                    }
                    float brightness = dot(normal, source)*-0.0002f;
                    tintMul = clamp(0.75f+brightness, 0.66f, 1.f);
                    if (prevTintAddition != voxelColor) {
                        prevTintAddition = voxelColor;
                        tint += voxelColor*tintMul;
                    }
                } else {
                    hitSolidVoxel = true;
                    texColor = baseColor;
                    return vec4(voxelColor.rgb, 1);
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
            vec3 entered = vec3(0.f);
            if (block.x == 1) {
                vec3 intersect = rayPos + rayDir * blockDist;
                vec3 uv3d = intersect - blockPos;
                if (uv3d == floor(voxelRayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - blockPos;
                }
                entered = floor(mapPos)+uv3d;
            }

            mask = stepMask(sideDist);
            blockPos += mask * raySign;
            sideDist += mask * raySign * deltaDist;
            mini = ((blockPos - rayPos) + 0.5 - 0.5 * vec3(raySign)) * deltaDist;
            blockDist = max(mini.x, max(mini.y, mini.z));
            mapPos = (lod2Pos*16)+(lodPos*4)+blockPos;
            block = inBounds(mapPos, worldSize) ? getBlock(mapPos.x, mapPos.y, mapPos.z) : (mapPos.y > 63 ? ivec4(0) : (mapPos.y == 63 ? ivec4(1, 14, 0, 0) : ivec4(0)));

            if (entered != vec3(0)) {
                vec3 intersect = rayPos + rayDir * blockDist;
                vec3 uv3d = intersect - blockPos;
                if (uv3d == floor(voxelRayPos)) { // Handle edge case where camera origin is inside of block
                    uv3d = rayPos - blockPos;
                }
                vec3 exited = floor(mapPos)+uv3d;
                float dist = distance(entered, exited);
                if (underwater && isShadow) {
                    waterDepth -= dist/30f;
                }
            }
        }
    }

    return vec4(0);
}

vec3 lodSize = vec3(size/4, height/4, size/4);
vec4 traceLOD(vec3 rayPos, vec3 iMask, float chunkDist) {
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
            vec3 prevRayDir = rayDir;
            vec4 voxelColor = traceBlock(uv3d, mask, lodDist, chunkDist);
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
vec4 raytrace(vec3 ogPos, vec3 newRayDir) {
    rayDir = roundDir(newRayDir);
    ogRayPos = ogPos;
    vec3 rayPos = ogPos/16;
    lod2Pos = floor(rayPos);
    vec3 raySign = sign(rayDir);
    vec3 deltaDist = 1.0/rayDir;
    vec3 sideDist = ((lod2Pos - rayPos) + 0.5 + raySign * 0.5) * deltaDist;
    vec3 mask = stepMask(sideDist);

    for (int i = 0; distance(rayPos, lod2Pos) < size/16 && i < size/8; i++) {
        mapPos = lod2Pos*16;
        bool inBound = inBounds(lod2Pos, lod2Size);
        if (!inBound && rayDir.y >= 0.f && inBounds(ogPos, worldSize)) {
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
            vec3 prevRayDir = rayDir;
            vec4 voxelColor = traceLOD(uv3d, mask, lod2Dist);
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

vec3 lightPos = vec3(0);
float shadowFactor = 1.f;
vec4 getShadow(vec4 color, bool actuallyCastShadowRay, bool isTracedObject, float dist) {
    if (!shadowsEnabled) {
        actuallyCastShadowRay=false;
    }
    if (actuallyCastShadowRay) {
        shadowFactor = 1.f;
    }
    float normalRounding = eigth*3;//*(1+min(3, dist/10));
    vec3 subbed = vec3(dot(normal.x, ogDir.x), dot(normal.y, ogDir.y), dot(normal.z, ogDir.z));
    bool xHighest = subbed.x > subbed.y && subbed.x > subbed.z;
    bool yHighest = subbed.y > subbed.x && subbed.y > subbed.z;
    bool zHighest = subbed.z > subbed.x && subbed.z > subbed.y;
    vec3 vNorm = normal;
    vec3 shadowPosOffset = vec3(0);
    vec3 blockPos = ivec3(solidHitPos)+0.5f;
    if (castsFullShadow(block) && isTracedObject) {
        vec3 avgNColor = vec3(0);
        float neighborsSolid = 0.f;
        bool wasY = false;
        vec4 above = fromLinear(getVoxelAndBlockWOLeavesOverride((solidHitPos+(normal*0.75f))+vec3(0, normalRounding, 0)));
        if (above.a < alphaMax) {
            vNorm.y = -1;
            shadowPosOffset.y = eigth;
            wasY = true;
        } else if (!yHighest) {
            float brightness = max(above.r, max(above.g, above.b));
            avgNColor.rgb += (above.rgb)*brightness;
            neighborsSolid+=1*brightness;
        }
        vec4 below = fromLinear(getVoxelAndBlockWOLeavesOverride((solidHitPos+(normal*0.75f))-vec3(0, normalRounding, 0)));
        if (below.a < alphaMax) {
            vNorm.y = wasY ? 0 : 1;
            shadowPosOffset.y = wasY ? 0 : -eigth;
        } else if (!yHighest) {
            float brightness = max(below.r, max(below.g, below.b));
            avgNColor.rgb += (below.rgb)*brightness;
            neighborsSolid+=1*brightness;
        }
        bool wasX = false;
        vec4 east = fromLinear(getVoxelAndBlockWOLeavesOverride((solidHitPos+(normal*0.75f))+vec3(normalRounding, 0, 0)));
        if (east.a < alphaMax) {
            vNorm.x = -1;
            shadowPosOffset.x = eigth;
            wasX = true;
        } else if (!xHighest) {
            float brightness = max(east.r, max(east.g, east.b));
            avgNColor.rgb += (east.rgb)*brightness;
            neighborsSolid+=1*brightness;
        }
        vec4 west = fromLinear(getVoxelAndBlockWOLeavesOverride((solidHitPos+(normal*0.75f))-vec3(normalRounding, 0, 0)));
        if (west.a < alphaMax) {
            vNorm.x = wasX ? 0 : 1;
            shadowPosOffset.x = wasX ? 0 : -eigth;
        } else if (!xHighest) {
            float brightness = max(west.r, max(west.g, west.b));
            avgNColor.rgb += (west.rgb)*brightness;
            neighborsSolid+=1*brightness;
        }
        if (!zHighest) {
            vec4 north = fromLinear(getVoxelAndBlockWOLeavesOverride((solidHitPos+(normal*0.75f))+vec3(0, 0, normalRounding)));
            if (north.a >= alphaMax) {
                float brightness = max(north.r, max(north.g, north.b));
                avgNColor.rgb += (north.rgb)*brightness;
                neighborsSolid+=1*brightness;
            }
            vec4 south = fromLinear(getVoxelAndBlockWOLeavesOverride((solidHitPos+(normal*0.75f))-vec3(0, 0, normalRounding)));
            if (south.a >= alphaMax) {
                float brightness = max(south.r, max(south.g, south.b));
                avgNColor.rgb += (south.rgb)*brightness;
                neighborsSolid+=1*brightness;
            }
        }
        if (neighborsSolid > 1) {
            avgNColor /= neighborsSolid;
            color.rgb = clamp(mix(color.rgb, avgNColor, clamp(min(dist/100, 1)-max(0, 4*(max(color.r, max(color.g, color.b))-0.5f)), 0, 1)), 0, 1);
        }
        if (texColor.a < 1 && texColor.a > alphaMax) {
            vNorm *= 0;
        }
    }
    vec3 shadowPos = mix((floor(prevPos*8)+0.5f)/8, prevPos, abs(normal))+(shadowPosOffset*2);
    if (actuallyCastShadowRay) {
        vec3 sunDir = vec3(normalize(source - (worldSize/2)));
        vec4 prevTint = tint;
        vec3 oldHitPos = hitPos;
        clearVars();
        isShadow = true;
        bool solidCaster = raytrace(shadowPos, sunDir).a > 0.0f;
        if (solidCaster) {
            if (waterDepth < 1.f) {
                waterDepth = 0.f;
            }
            shadowFactor = min(0.9f, mix(0.75f, 0.9f, min(1, distance(shadowPos, hitPos)/420)));
        }
        isShadow = false;
        tint = prevTint;
        hitPos = oldHitPos;
    }
    float brightness = (dot(vNorm, source)*-0.0002f)*waterDepth;
    color.rgb *= clamp(0.75f+brightness, 0.66f, 1.f);
    return color;
}

const float[16] xOffsets = float[16](0.0f, -0.25f, 0.25f, -0.375f, 0.125f, -0.125f, 0.375f, -0.4375f, 0.0625f, -0.1875f, 0.3125f, -0.3125f, 0.1875f, -0.0625f, 0.4375f, -0.46875f);
const float[16] yOffsets = float[16](0.0f, 0.166667f, -0.388889f, -0.055556f, 0.277778f, -0.277778f, 0.055556f, 0.388889f, -0.462963f, -0.12963f, 0.203704f, -0.351852f, -0.018519f, 0.314815f, -0.240741f, 0.092593f);
const float nearClip = 0.01f;

vec3 getDir() {
    vec2 screenSpace = (gl_FragCoord.xy+vec2(xOffsets[offsetIdx], yOffsets[offsetIdx])) / res;
    vec4 clipSpace = vec4(screenSpace * 2.0f - 1.0f, -1.0f, 1.0f);
    vec4 eyeSpace = vec4(vec2(inverse(projection) * clipSpace), -1.0f, 0.0f);
    return normalize(vec3(inverse(view)*eyeSpace));
}

bool skyChecks() {
    bool isSky = false;
    if (solidHitPos != vec3(0)) {
        isSky = false;
    }
    if (fragColor.a < alphaMax) {
        isSky = true;
    }
    if (isSky) {
        solidHitPos = mapPos;
    }
    return isSky;
}

void main() {
    vec2 pos = upscale ? ivec2(gl_FragCoord.x*2, gl_FragCoord.y) : ivec2(gl_FragCoord.xy);
    bool yOdd = bool(int(gl_FragCoord.y) % 2 == 1);
    vec2 normalizedPos = (upscale ? vec2(pos.x+(yOdd ? 1 : 0), pos.y) : pos)/res;
    if (taa) {
        float xOff = xOffsets[offsetIdx];
        float yOff = yOffsets[offsetIdx];
        pos.x += xOff;
        pos.y += yOff;
    }
    mat4 invView = inverse(view);
    vec2 uv = ((pos / res)*2.f)-1.f;
    vec4 clipSpace = vec4((inverse(projection) * vec4(uv, 1.f, 1.f)).xyz, 0);
    ogDir = roundDir(normalize((invView*clipSpace).xyz));
    ogPos = invView[3].xyz;
    vec4 rasterColor = texture(raster_color, pos/res);
    vec4 rasterPos = texture(raster_pos, pos/res);
    source = mun.y > sun.y ? mun : sun;
    source.y = max(source.y, 500);
    source.z += 128;
    bool isSky = rasterColor.a <= 0.f;
    bool isLight = false;
    updateLightFog(ogPos);
    if (rasterPos.w < 0.05) {
        if (rasterColor.a >= 1) {
            maxRayDist = distance(ogPos, rasterPos.xyz);
        }
        fragColor = raytrace(ogPos, ogDir);
        if (isLightSource(block.xy) && max(texColor.r, max(texColor.g, texColor.b)) > 0.9f) {
            fragColor.rgb *= 1.5f;
            isLight = true;
        }
        isSky = skyChecks();
        if (mapPos.y <= 63 && fragColor.a < alphaMax) {
            fragColor = vec4(1.0f, 0.0f, 0.0f, 1.f);
            isSky = false;
        }
    }
    shouldSelectBlock = false;
    if (hitBlock == ivec3(playerData[0], playerData[1], playerData[2]) && ui) {
        fragColor.rgb = mix(fragColor.rgb, vec3(0.7, 0.7, 1), 0.5f);
    }
    vec4 lighting = vec4(-1);
    lightPos = ogPos + ogDir * size; //this is only used if it's the sky
    bool isTracedObject = true;
    if (distance(ogPos, rasterPos.xyz) < distance(ogPos, hitPos+(normal/2)) || fragColor.a < alphaMax) {
        if (rasterPos.y > 63 || (rasterPos.y < height && rasterPos.x > 0 && rasterPos.x < size && rasterPos.z > 0 && rasterPos.z < size)) { //if out of bounds, only render when above sea level.
            isTracedObject = false;
            fragColor.rgb = fromLinear(rasterColor).rgb;
            fragColor.a = rasterColor.a;
            texColor = fragColor;
            normal = texture(raster_norm, normalizedPos).xyz;
            solidHitPos = rasterPos.xyz-(normal/2);
            prevPos = rasterPos.xyz-(normal*0.002f);
            isSky = false;
            isLight = max(rasterColor.r, max(rasterColor.g, rasterColor.b)) >= 1.f;
        }
    }
    vec3 dPos = solidHitPos+(normal/2)-ogPos;
    float depth = nearClip/max(0, dot(dPos, ogDir));
    if (inBounds(solidHitPos, worldSize)) {
        lighting = fromLinear(getLight(solidHitPos.x, solidHitPos.y, solidHitPos.z));
    } else {
        lighting = fromLinear(vec4(0, 0, 0, 1));
    }
    if (!isSky) {
        lightPos = solidHitPos;
        if (!isLight) {
            fragColor = getShadow(fragColor, true, isTracedObject && !isSky && hitSolidVoxel, distance(ogPos, solidHitPos));
        } else if (fragColor.a >= 10) {
            fragColor.a -= 10;
            shadowFactor = 0.75f;
        }
    }
    float fogginess = clamp((clamp(sqrt(distance(ogPos, lightPos)/(size*0.66f))*gradient(lightPos.y, 63, 80, 1, 1+abs(noise(lightPos.xz)/3)), 0, 1)), 0.f, 1.f);
    if (fragColor.a < 0 && !isSky) {fogginess *= 0.5f;}
    lighting.a = mix(lighting.a*shadowFactor, fromLinear(vec4(0, 0, 0, 1)).a, fogginess);
    lighting = powLighting(lighting);
    if (!isLight) {
        fogDetractorFactor = -1;
        lighting.a*=waterDepth;
        vec4 lightingColor = getLightingColor(lightPos, lighting, isSky, fogginess);
        fragColor.rgb *= lightingColor.rgb;
        fragColor.rgb = mix(fragColor.rgb*1.2f, lightingColor.rgb, fogginess);
    }
    if (tint.a > 0) {
        lightPos = hitPos;
        normal = tintNormal;
        vec4 normalizedTint = tint/max(1.f, max(tint.r, max(tint.g, tint.b)));
        normalizedTint = getShadow(normalizedTint, false, false, 0.f);
        fogginess = clamp((clamp(sqrt(distance(ogPos, lightPos)/(size*0.66f))*gradient(lightPos.y, 63, 80, 1, 1+abs(noise(lightPos.xz)/3)), 0, 1)), 0.f, 1.f);
        lighting = fromLinear(getLight(lightPos.x, lightPos.y, lightPos.z));
        lighting.a = mix(lighting.a*shadowFactor, fromLinear(vec4(0, 0, 0, 1)).a, fogginess);
        lighting = powLighting(lighting);
        vec4 lightingColor = getLightingColor(lightPos, lighting, false, fogginess);
        normalizedTint.rgb *= lightingColor.rgb;
        normalizedTint.rgb = mix(normalizedTint.rgb*1.2f, lightingColor.rgb, fogginess);
        float reflectivity = dot(normal, ogDir);
        fragColor.rgb = mix(fragColor.rgb, normalizedTint.rgb, mix(1.f, normalizedTint.a, reflectivity));
    }
    fragColor.rgb += max(vec3(0), mix(lightFog.rgb, vec3(0), fogDetractorFactor));
    fragColor = toLinear(fragColor);
    fragColor.a = depth;
}