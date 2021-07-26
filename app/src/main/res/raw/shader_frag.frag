precision highp float;

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Danny Lin <danny@kdrag0n.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


/*
 * Config
 */

const float HUE_RATE = 20.0;

const float SRGB_WHITE_LUMINANCE = 203.0; // cd/m^2


/*
 * Helpers
 */

const float PI = 3.141592653589793;
const float FLT_MAX = 3.402823466e+38;

float degreesToRadians(float x) {
    return x * PI / 180.0;
}

float radiansToDegrees(float x) {
    return x * 180.0 / PI;
}

float atan2(float y, float x) {
    bool s = (abs(x) > abs(y));
    return mix(PI/2.0 - atan(x,y), atan(y,x), s);
}

float cbrt(float x) {
    if (x > 0.0) {
        return pow(x, 1.0 / 3.0);
    } else if (x < 0.0) {
        return -pow(-x, 1.0 / 3.0);
    } else {
        return 0.0;
    }
}

float square(float x) {
    return x * x;
}

float cube(float x) {
    return x * x * x;
}


/*
 * Number rendering
 */
// ---- 8< ---- GLSL Number Printing - @P_Malin ---- 8< ----
// Creative Commons CC0 1.0 Universal (CC-0)
// https://www.shadertoy.com/view/4sBSWW

float DigitBin( const int x )
{
    return x==0?480599.0:x==1?139810.0:x==2?476951.0:x==3?476999.0:x==4?350020.0:x==5?464711.0:x==6?464727.0:x==7?476228.0:x==8?481111.0:x==9?481095.0:0.0;
}

float PrintValue( vec2 vStringCoords, float fValue, float fMaxDigits, float fDecimalPlaces )
{
    if ((vStringCoords.y < 0.0) || (vStringCoords.y >= 1.0)) return 0.0;

    bool bNeg = ( fValue < 0.0 );
    fValue = abs(fValue);

    float fLog10Value = log2(abs(fValue)) / log2(10.0);
    float fBiggestIndex = max(floor(fLog10Value), 0.0);
    float fDigitIndex = fMaxDigits - floor(vStringCoords.x);
    float fCharBin = 0.0;
    if(fDigitIndex > (-fDecimalPlaces - 1.01)) {
        if(fDigitIndex > fBiggestIndex) {
            if((bNeg) && (fDigitIndex < (fBiggestIndex+1.5))) fCharBin = 1792.0;
        } else {
            if(fDigitIndex == -1.0) {
                if(fDecimalPlaces > 0.0) fCharBin = 2.0;
            } else {
                float fReducedRangeValue = fValue;
                if(fDigitIndex < 0.0) { fReducedRangeValue = fract( fValue ); fDigitIndex += 1.0; }
                float fDigitValue = (abs(fReducedRangeValue / (pow(10.0, fDigitIndex))));
                fCharBin = DigitBin(int(floor(mod(fDigitValue, 10.0))));
            }
        }
    }
    return floor(mod((fCharBin / pow(2.0, floor(fract(vStringCoords.x) * 4.0) + (floor(vStringCoords.y * 5.0) * 4.0))), 2.0));
}

// ---- 8< -------- 8< -------- 8< -------- 8< ----


/*
 * LCh
 */

vec3 labToLch(vec3 c) {
    float L = c.x;
    float a = c.y;
    float b = c.z;

    float hDeg = radiansToDegrees(atan2(b, a));
    return vec3(
        L,
        sqrt(a*a + b*b),
        (hDeg < 0.0) ? hDeg + 360.0 : hDeg
    );
}

vec3 lchToLab(vec3 c) {
    float L = c.x;
    float C = c.y;
    float h = c.z;

    float hRad = degreesToRadians(h);
    return vec3(
        L,
        C * cos(hRad),
        C * sin(hRad)
    );
}


/*
 * sRGB
 */

vec3 srgbTransfer(vec3 c) {
    vec3 gamma = vec3(1.055) * pow(c, vec3(1.0/2.4)) - vec3(0.055);
    vec3 linear = vec3(12.92) * c;
    bvec3 selectParts = lessThan(c, vec3(0.0031308));
    return mix(gamma, linear, selectParts);
}

vec3 srgbTransferInv(vec3 c) {
    vec3 gamma = pow((c + vec3(0.055))/vec3(1.0 + 0.055), vec3(2.4));
    vec3 linear = c / vec3(12.92);
    bvec3 selectParts = lessThan(c, vec3(0.04045));
    return mix(gamma, linear, selectParts);
}

bool linearSrgbInGamut(vec3 c) {
    vec3 clamped = clamp(c, 0.0, 1.0);
    return c == clamped;
}


/*
 * XYZ
 */

const vec3 D65 = vec3(0.95047, 1.0, 1.08883);

vec3 linearSrgbToXyz(vec3 c) {
    float r = c.x;
    float g = c.y;
    float b = c.z;

    return vec3(
        0.4124564 * r + 0.3575761 * g + 0.1804375 * b,
        0.2126729 * r + 0.7151522 * g + 0.0721750 * b,
        0.0193339 * r + 0.1191920 * g + 0.9503041 * b
    );
}

vec3 xyzToLinearSrgb(vec3 c) {
    return vec3(
        +3.2404542 * c.x + -1.5371385 * c.y + -0.4985314 * c.z,
        -0.9692660 * c.x + +1.8760108 * c.y + +0.0415560 * c.z,
        +0.0556434 * c.x + -0.2040259 * c.y + +1.0572252 * c.z
    );
}


/*
 * CIELAB
 */


float cielabF(float x) {
    if (x > 216.0/24389.0) {
        return cbrt(x);
    } else {
        return x / (108.0/841.0) + 4.0/29.0;
    }
}

float cielabFInv(float x) {
    if (x > 6.0/29.0) {
        return cube(x);
    } else {
        return (108.0/841.0) * (x - 4.0/29.0);
    }
}

vec3 xyzToCielab(vec3 c) {
    float L = 116.0 * cielabF(c.y / D65.y) - 16.0;
    float a = 500.0 * (cielabF(c.x / D65.x) - cielabF(c.y / D65.y));
    float b = 200.0 * (cielabF(c.y / D65.y) - cielabF(c.z / D65.z));
    return vec3(L, a, b);
}

vec3 cielabToXyz(vec3 c) {
    float L = c.x;
    float a = c.y;
    float b = c.z;

    float lp = (L + 16.0) / 116.0;
    float x = D65.x * cielabFInv(lp + (a / 500.0));
    float y = D65.y * cielabFInv(lp);
    float z = D65.z * cielabFInv(lp - (b / 200.0));
    return vec3(x, y, z);
}


/*
 * Oklab
 */

vec3 linearSrgbToOklab(vec3 c) {
    float r = c.x;
    float g = c.y;
    float b = c.z;

    float l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b;
    float m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b;
    float s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b;

    float l_ = cbrt(l);
    float m_ = cbrt(m);
    float s_ = cbrt(s);

    return vec3(
        0.2104542553f*l_ + 0.7936177850f*m_ - 0.0040720468f*s_,
        1.9779984951f*l_ - 2.4285922050f*m_ + 0.4505937099f*s_,
        0.0259040371f*l_ + 0.7827717662f*m_ - 0.8086757660f*s_
    );
}

vec3 oklabToLinearSrgb(vec3 c) {
    float L = c.x;
    float a = c.y;
    float b = c.z;

    float l_ = L + 0.3963377774f * a + 0.2158037573f * b;
    float m_ = L - 0.1055613458f * a - 0.0638541728f * b;
    float s_ = L - 0.0894841775f * a - 1.2914855480f * b;

    float l = l_*l_*l_;
    float m = m_*m_*m_;
    float s = s_*s_*s_;

    return vec3(
        +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    );
}


/*
 * Oklab gamut clipping
 */

/*
 * Ported from the original C/C++ implementation:
 *
 * Copyright (c) 2021 Björn Ottosson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// Finds the maximum saturation possible for a given hue that fits in sRGB
// Saturation here is defined as S = C/L
// a and b must be normalized so a^2 + b^2 == 1
float compute_max_saturation(float a, float b)
{
    // Max saturation will be when one of r, g or b goes below zero.

    // Select different coefficients depending on which component goes below zero first
    float k0, k1, k2, k3, k4, wl, wm, ws;

    if (-1.88170328f * a - 0.80936493f * b > 1.0)
    {
        // Red component
        k0 = +1.19086277f; k1 = +1.76576728f; k2 = +0.59662641f; k3 = +0.75515197f; k4 = +0.56771245f;
        wl = +4.0767416621f; wm = -3.3077115913f; ws = +0.2309699292f;
    }
    else if (1.81444104f * a - 1.19445276f * b > 1.0)
    {
        // Green component
        k0 = +0.73956515f; k1 = -0.45954404f; k2 = +0.08285427f; k3 = +0.12541070f; k4 = +0.14503204f;
        wl = -1.2681437731f; wm = +2.6097574011f; ws = -0.3413193965f;
    }
    else
    {
        // Blue component
        k0 = +1.35733652f; k1 = -0.00915799f; k2 = -1.15130210f; k3 = -0.50559606f; k4 = +0.00692167f;
        wl = -0.0041960863f; wm = -0.7034186147f; ws = +1.7076147010f;
    }

    // Approximate max saturation using a polynomial:
    float S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b;

    // Do one step Halley's method to get closer
    // this gives an error less than 10e6, except for some blue hues where the dS/dh is close to infinite
    // this should be sufficient for most applications, otherwise do two/three steps

    float k_l = +0.3963377774f * a + 0.2158037573f * b;
    float k_m = -0.1055613458f * a - 0.0638541728f * b;
    float k_s = -0.0894841775f * a - 1.2914855480f * b;

    {
        float l_ = 1.f + S * k_l;
        float m_ = 1.f + S * k_m;
        float s_ = 1.f + S * k_s;

        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;

        float l_dS = 3.f * k_l * l_ * l_;
        float m_dS = 3.f * k_m * m_ * m_;
        float s_dS = 3.f * k_s * s_ * s_;

        float l_dS2 = 6.f * k_l * k_l * l_;
        float m_dS2 = 6.f * k_m * k_m * m_;
        float s_dS2 = 6.f * k_s * k_s * s_;

        float f  = wl * l     + wm * m     + ws * s;
        float f1 = wl * l_dS  + wm * m_dS  + ws * s_dS;
        float f2 = wl * l_dS2 + wm * m_dS2 + ws * s_dS2;

        S = S - f * f1 / (f1*f1 - 0.5f * f * f2);
    }

    return S;
}

// finds L_cusp and C_cusp for a given hue
// a and b must be normalized so a^2 + b^2 == 1
vec2 find_cusp(float a, float b)
{
    // First, find the maximum saturation (saturation S = C/L)
    float S_cusp = compute_max_saturation(a, b);

    // Convert to linear sRGB to find the first point where at least one of r,g or b >= 1:
    vec3 rgb_at_max = oklabToLinearSrgb(vec3( 1.0, S_cusp * a, S_cusp * b ));
    float L_cusp = cbrt(1.f / max(max(rgb_at_max.r, rgb_at_max.g), rgb_at_max.b));
    float C_cusp = L_cusp * S_cusp;

    return vec2( L_cusp , C_cusp );
}

// Finds intersection of the line defined by
// L = L0 * (1 - t) + t * L1;
// C = t * C1;
// a and b must be normalized so a^2 + b^2 == 1
float find_gamut_intersection(float a, float b, float L1, float C1, float L0)
{
    // Find the cusp of the gamut triangle
    vec2 cusp = find_cusp(a, b);
    float cuspL = cusp.x;
    float cuspC = cusp.y;

    // Find the intersection for upper and lower half seprately
    float t;
    if (((L1 - L0) * cuspC - (cuspL - L0) * C1) <= 0.f)
    {
        // Lower half

        t = cuspC * L0 / (C1 * cuspL + cuspC * (L0 - L1));
    }
    else
    {
        // Upper half

        // First intersect with triangle
        t = cuspC * (L0 - 1.f) / (C1 * (cuspL - 1.f) + cuspC * (L0 - L1));

        // Then one step Halley's method
        {
            float dL = L1 - L0;
            float dC = C1;

            float k_l = +0.3963377774f * a + 0.2158037573f * b;
            float k_m = -0.1055613458f * a - 0.0638541728f * b;
            float k_s = -0.0894841775f * a - 1.2914855480f * b;

            float l_dt = dL + dC * k_l;
            float m_dt = dL + dC * k_m;
            float s_dt = dL + dC * k_s;

            // If higher accuracy is required, 2 or 3 iterations of the following block can be used:
            {
                float L = L0 * (1.f - t) + t * L1;
                float C = t * C1;

                float l_ = L + C * k_l;
                float m_ = L + C * k_m;
                float s_ = L + C * k_s;

                float l = l_ * l_ * l_;
                float m = m_ * m_ * m_;
                float s = s_ * s_ * s_;

                float ldt = 3.0 * l_dt * l_ * l_;
                float mdt = 3.0 * m_dt * m_ * m_;
                float sdt = 3.0 * s_dt * s_ * s_;

                float ldt2 = 6.0 * l_dt * l_dt * l_;
                float mdt2 = 6.0 * m_dt * m_dt * m_;
                float sdt2 = 6.0 * s_dt * s_dt * s_;

                float r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s - 1.0;
                float r1 = 4.0767416621f * ldt - 3.3077115913f * mdt + 0.2309699292f * sdt;
                float r2 = 4.0767416621f * ldt2 - 3.3077115913f * mdt2 + 0.2309699292f * sdt2;

                float u_r = r1 / (r1 * r1 - 0.5f * r * r2);
                float t_r = -r * u_r;

                float g = -1.2681437731f * l + 2.6097574011f * m - 0.3413193965f * s - 1.0;
                float g1 = -1.2681437731f * ldt + 2.6097574011f * mdt - 0.3413193965f * sdt;
                float g2 = -1.2681437731f * ldt2 + 2.6097574011f * mdt2 - 0.3413193965f * sdt2;

                float u_g = g1 / (g1 * g1 - 0.5f * g * g2);
                float t_g = -g * u_g;

                float b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s - 1.0;
                float b1 = -0.0041960863f * ldt - 0.7034186147f * mdt + 1.7076147010f * sdt;
                float b2 = -0.0041960863f * ldt2 - 0.7034186147f * mdt2 + 1.7076147010f * sdt2;

                float u_b = b1 / (b1 * b1 - 0.5f * b * b2);
                float t_b = -b * u_b;

                t_r = u_r >= 0.f ? t_r : FLT_MAX;
                t_g = u_g >= 0.f ? t_g : FLT_MAX;
                t_b = u_b >= 0.f ? t_b : FLT_MAX;

                t += min(t_r, min(t_g, t_b));
            }
        }
    }

    return t;
}

vec3 gamut_clip_preserve_lightness(vec3 rgb)
{
    if (rgb.r < 1.0 && rgb.g < 1.0 && rgb.b < 1.0 && rgb.r > 0.0 && rgb.g > 0.0 && rgb.b > 0.0)
        return rgb;

    vec3 lab = linearSrgbToOklab(rgb);

    float L = lab.x;
    float eps = 0.00001f;
    float C = max(eps, sqrt(lab.y * lab.y + lab.z * lab.z));
    float a_ = lab.y / C;
    float b_ = lab.z / C;

    float L0 = clamp(L, 0.0, 1.0);

    float t = find_gamut_intersection(a_, b_, L, C, L0);
    float L_clipped = L0 * (1.0 - t) + t * L;
    float C_clipped = t * C;

    return oklabToLinearSrgb(vec3( L_clipped, C_clipped * a_, C_clipped * b_ ));
}

vec3 gamut_clip_project_to_0_5(vec3 rgb)
{
    if (rgb.r < 1.0 && rgb.g < 1.0 && rgb.b < 1.0 && rgb.r > 0.0 && rgb.g > 0.0 && rgb.b > 0.0)
        return rgb;

    vec3 lab = linearSrgbToOklab(rgb);

    float L = lab.x;
    float eps = 0.00001f;
    float C = max(eps, sqrt(lab.y * lab.y + lab.z * lab.z));
    float a_ = lab.y / C;
    float b_ = lab.z / C;

    float L0 = 0.5;

    float t = find_gamut_intersection(a_, b_, L, C, L0);
    float L_clipped = L0 * (1.0 - t) + t * L;
    float C_clipped = t * C;

    return oklabToLinearSrgb(vec3( L_clipped, C_clipped * a_, C_clipped * b_ ));
}

vec3 gamut_clip_project_to_L_cusp(vec3 rgb)
{
    if (rgb.r < 1.0 && rgb.g < 1.0 && rgb.b < 1.0 && rgb.r > 0.0 && rgb.g > 0.0 && rgb.b > 0.0)
        return rgb;

    vec3 lab = linearSrgbToOklab(rgb);

    float L = lab.x;
    float eps = 0.00001f;
    float C = max(eps, sqrt(lab.y * lab.y + lab.z * lab.z));
    float a_ = lab.y / C;
    float b_ = lab.z / C;

    // The cusp is computed here and in find_gamut_intersection, an optimized solution would only compute it once.
    vec2 cusp = find_cusp(a_, b_);
    float cuspL = cusp.x;
    float cuspC = cusp.y;

    float L0 = cuspL;

    float t = find_gamut_intersection(a_, b_, L, C, L0);

    float L_clipped = L0 * (1.0 - t) + t * L;
    float C_clipped = t * C;

    return oklabToLinearSrgb(vec3( L_clipped, C_clipped * a_, C_clipped * b_ ));
}

vec3 gamut_clip_adaptive_L0_0_5(vec3 rgb, float alpha)
{
    if (rgb.r < 1.0 && rgb.g < 1.0 && rgb.b < 1.0 && rgb.r > 0.0 && rgb.g > 0.0 && rgb.b > 0.0)
        return rgb;

    vec3 lab = linearSrgbToOklab(rgb);

    float L = lab.x;
    float eps = 0.00001f;
    float C = max(eps, sqrt(lab.y * lab.y + lab.z * lab.z));
    float a_ = lab.y / C;
    float b_ = lab.z / C;

    float Ld = L - 0.5f;
    float e1 = 0.5f + abs(Ld) + alpha * C;
    float L0 = 0.5f*(1.f + sign(Ld)*(e1 - sqrt(e1*e1 - 2.f *abs(Ld))));

    float t = find_gamut_intersection(a_, b_, L, C, L0);
    float L_clipped = L0 * (1.f - t) + t * L;
    float C_clipped = t * C;

    return oklabToLinearSrgb(vec3( L_clipped, C_clipped * a_, C_clipped * b_ ));
}

vec3 gamut_clip_adaptive_L0_L_cusp(vec3 rgb, float alpha)
{
    if (rgb.r < 1.0 && rgb.g < 1.0 && rgb.b < 1.0 && rgb.r > 0.0 && rgb.g > 0.0 && rgb.b > 0.0)
        return rgb;

    vec3 lab = linearSrgbToOklab(rgb);

    float L = lab.x;
    float eps = 0.00001f;
    float C = max(eps, sqrt(lab.y * lab.y + lab.z * lab.z));
    float a_ = lab.y / C;
    float b_ = lab.z / C;

    // The cusp is computed here and in find_gamut_intersection, an optimized solution would only compute it once.
    vec2 cusp = find_cusp(a_, b_);
    float cuspL = cusp.x;
    float cuspC = cusp.y;

    float Ld = L - cuspL;
    float k = 2.f * (Ld > 0.0 ? 1.f - cuspL : cuspL);

    float e1 = 0.5f*k + abs(Ld) + alpha * C/k;
    float L0 = cuspL + 0.5f * (sign(Ld) * (e1 - sqrt(e1 * e1 - 2.f * k * abs(Ld))));

    float t = find_gamut_intersection(a_, b_, L, C, L0);
    float L_clipped = L0 * (1.f - t) + t * L;
    float C_clipped = t * C;

    return oklabToLinearSrgb(vec3( L_clipped, C_clipped * a_, C_clipped * b_ ));
}


/*
 * ZCAM (JMh values)
 */

const float B = 1.15;
const float G = 0.66;
const float C1 = 3424.0 / 4096.0;
const float C2 = 2413.0 / 128.0;
const float C3 = 2392.0 / 128.0;
const float ETA = 2610.0 / 16384.0;
const float RHO = 1.7 * 2523.0 / 32.0;
const float EPSILON = 3.7035226210190005e-11;

const float SURROUND_DARK = 0.525;
const float SURROUND_DIM = 0.59;
const float SURROUND_AVERAGE = 0.69;

struct ZcamViewingConditions {
    // Given
    float F_s;
    float L_a;
    float Y_b;
    vec3 refWhite;

    // Calculated
    float F_b;
    float F_l;
    float refWhiteIz;
};

float pq(float x) {
    float num = C1 + C2 * pow(x / 10000.0, ETA);
    float denom = 1.0 + C3 * pow(x / 10000.0, ETA);

    return pow(num / denom, RHO);
}

float pqInv(float x) {
    float num = C1 - pow(x, 1.0/RHO);
    float denom = C3*pow(x, 1.0/RHO) - C2;

    return 10000.0 * pow(num / denom, 1.0/ETA);
}

vec3 xyzToIzazbz(vec3 c) {
    float xp = B*c.x - (B-1.0)*c.z;
    float yp = G*c.y - (G-1.0)*c.x;

    float rp = pq( 0.41478972*xp + 0.579999*yp + 0.0146480*c.z);
    float gp = pq(-0.20151000*xp + 1.120649*yp + 0.0531008*c.z);
    float bp = pq(-0.01660080*xp + 0.264800*yp + 0.6684799*c.z);

    float az = 3.524000*rp + -4.066708*gp +  0.542708*bp;
    float bz = 0.199076*rp +  1.096799*gp + -1.295875*bp;
    float Iz = gp - EPSILON;

    return vec3(Iz, az, bz);
}

float hpToEz(float hp) {
    return 1.015 + cos(degreesToRadians(89.038 + hp));
}

float izToQz(float Iz, ZcamViewingConditions cond) {
    return 2700.0 * pow(Iz, (1.6 * cond.F_s) / pow(cond.F_b, 0.12)) *
            (pow(cond.F_s, 2.2) * pow(cond.F_b, 0.5) * pow(cond.F_l, 0.2));
}

ZcamViewingConditions createZcamViewingConditions(float F_s, float L_a, float Y_b, vec3 refWhite) {
    float F_b = sqrt(Y_b / refWhite.y);
    float F_l = 0.171 * cbrt(L_a) * (1.0 - exp(-48.0/9.0 * L_a));
    float refWhiteIz = xyzToIzazbz(refWhite).x;

    return ZcamViewingConditions(
        F_s, L_a, Y_b, refWhite,
        F_b, F_l, refWhiteIz
    );
}

vec3 zcamToXyz(vec3 c, ZcamViewingConditions cond) {
    float Jz = c.x;
    float Vz = c.y;
    float hz = c.z;

    float Iz_w = cond.refWhiteIz;
    float Qz_w = izToQz(Iz_w, cond);

    /* Step 1 */
    // Achromatic response
    float Iz_denom = 2700.0 * pow(cond.F_s, 2.2) * pow(cond.F_b, 0.5) * pow(cond.F_l, 0.2);
    float Iz_src = (Jz * Qz_w) / (Iz_denom * 100.0);
    float Iz = pow(Iz_src, pow(cond.F_b, 0.12) / (1.6 * cond.F_s));

    /* Step 2 */
    // Chroma
    float Cz = sqrt((square(Vz) - square(Jz - 58.0)) / 3.4);

    /* Step 3 is missing because hue composition is not supported */

    /* Step 4 */
    // ... and back to colorfulness
    /* Mz comes from input */
    float Mz = (Cz * Qz_w) / 100.0;
    float ez = hpToEz(hz);
    float Cz_p = pow((Mz * pow(Iz_w, 0.78) * pow(cond.F_b, 0.1)) /
            // Paper specifies pow(1.3514) but this extra precision is necessary for more accurate inversion
            (100.0 * pow(ez, 0.068) * pow(cond.F_l, 0.2)), 1.0 / 0.37 / 2.0);
    float az = Cz_p * cos(degreesToRadians(hz));
    float bz = Cz_p * sin(degreesToRadians(hz));

    /* Step 5 */
    float I = Iz + EPSILON;

    float r = pqInv(I + 0.2772100865*az +  0.1160946323*bz);
    float g = pqInv(I);
    float b = pqInv(I + 0.0425858012*az + -0.7538445799*bz);

    float xp =  1.9242264358*r + -1.0047923126*g +  0.0376514040*b;
    float yp =  0.3503167621*r +  0.7264811939*g + -0.0653844229*b;
    float z  = -0.0909828110*r + -0.3127282905*g +  1.5227665613*b;

    float x = (xp + (B - 1.0)*z) / B;
    float y = (yp + (G - 1.0)*x) / G;

    return vec3(x, y, z);
}

vec3 zcamJmhToLinearSrgb(vec3 jmh, ZcamViewingConditions cond) {
    vec3 xyzAbs = zcamToXyz(jmh, cond);
    vec3 xyzRel = xyzAbs / SRGB_WHITE_LUMINANCE;
    return xyzToLinearSrgb(xyzRel);
}

const float ZCAM_CHROMA_EPSILON = 0.0001;
const bool CLIP_ZCAM = false; // warning: can crash GPU!
vec3 clipZcamJmhToLinearSrgb(vec3 jmh, ZcamViewingConditions cond) {
    vec3 initialResult = zcamJmhToLinearSrgb(jmh, cond);
    if (linearSrgbInGamut(initialResult)) {
        return initialResult;
    }

    float lightness = jmh.r;
    float chroma = jmh.g;
    float hue = jmh.b;
    if (lightness <= ZCAM_CHROMA_EPSILON) {
        return vec3(0.0);
    } else if (lightness >= 100.0 - ZCAM_CHROMA_EPSILON) {
        return vec3(1.0);
    }

    float lo = 0.0;
    float hi = chroma;

    vec3 newLinearSrgb = initialResult;
    while (abs(hi - lo) > EPSILON) {
        float mid = (lo + hi) / 2.0;

        newLinearSrgb = zcamJmhToLinearSrgb(vec3(lightness, mid, hue), cond);
        if (!linearSrgbInGamut(newLinearSrgb)) {
            hi = mid;
        } else {
            float mid2 = mid + EPSILON;

            vec3 newLinearSrgb2 = zcamJmhToLinearSrgb(vec3(lightness, mid2, hue), cond);
            if (linearSrgbInGamut(newLinearSrgb2)) {
                lo = mid;
            } else {
                break;
            }
        }
    }

    return newLinearSrgb;
}


/*
 * Theme generation
 */

const float OKLAB_ACCENT1_CHROMA = 0.1328123146401862;
const float OKLAB_LIGHTNESS_MAP[13] = float[](
    1.0,
    0.9880873963836093,
    0.9551400440214246,
    0.9127904082618294,
    0.8265622041716898,
    0.7412252673769428,
    0.653350946076347,
    0.5624050605208273,
    0.48193149058901036,
    0.39417829080418526,
    0.3091856317280812,
    0.22212874192541768,
    0.0
);

const float ZCAM_ACCENT1_CHROMA = 20.54486422; // careful!
const float ZCAM_ACCENT1_COLORFULNESS = 36.47983487;
const float ZCAM_LIGHTNESS_MAP[13] = float[](
    100.00000296754273,
    98.60403974009428,
    94.72386350388908,
    89.69628870011267,
    79.3326296037671,
    68.938947819272,
    58.15091644790415,
    46.991689840263206,
    37.24709908558773,
    26.96785892507836,
    17.67571012446932,
    9.36696155986009,
    0.0
);

const float SWATCH_CHROMA_SCALES[5] = float[](
    1.0, // accent1
    1.0 / 3.0, // accent2
    (1.0 / 3.0) * 2.0, // accent3
    1.0 / 12.0, // neutral1
    (1.0 / 12.0) * 2.0 // neutral2
);

vec3 calcShadeParams(int swatch, int shade, float seedChroma, float seedHue, float chromaFactor, float lightnessMap[13], float accent1Chroma) {
    float lightness = lightnessMap[shade];
    float refChroma = accent1Chroma * SWATCH_CHROMA_SCALES[0];
    float targetChroma = accent1Chroma * SWATCH_CHROMA_SCALES[swatch];
    float scaleC = (refChroma == 0.0) ? 0.0 : (clamp(seedChroma, 0.0, refChroma) / refChroma);
    float chroma = targetChroma * scaleC * chromaFactor;
    float hue = (swatch == 2) ? seedHue + 60.0 : seedHue;

    return vec3(lightness, chroma, hue);
}

vec3 generateShadeOklab(int swatch, int shade, float seedChroma, float seedHue, float chromaFactor) {
    vec3 lch = calcShadeParams(swatch, shade, seedChroma, seedHue, chromaFactor, OKLAB_LIGHTNESS_MAP, OKLAB_ACCENT1_CHROMA);
    vec3 oklab = lchToLab(lch);
    return oklabToLinearSrgb(oklab);
}

vec3 generateShadeZcam(int swatch, int shade, float seedChroma, float seedHue, float chromaFactor) {
    ZcamViewingConditions cond = createZcamViewingConditions(SURROUND_AVERAGE, 40.0, 20.0, D65 * SRGB_WHITE_LUMINANCE);

    vec3 jmh = calcShadeParams(swatch, shade, seedChroma, seedHue, chromaFactor, ZCAM_LIGHTNESS_MAP, ZCAM_ACCENT1_COLORFULNESS);

    if (CLIP_ZCAM) {
        return clipZcamJmhToLinearSrgb(jmh, cond);
    } else {
        return zcamJmhToLinearSrgb(jmh, cond);
    }
}

vec3 getThemeColor(vec2 uv, float hue) {
    int shadeIdx = int(uv.x * 13.0);
    int swatchIdx = int((1.0 - uv.y) * 5.0);
    float seedChroma = 1000000.0;

    if (iMouse.z > 0.0) {
        return generateShadeZcam(swatchIdx, shadeIdx, seedChroma, hue, 1.0);
    } else {
        return generateShadeOklab(swatchIdx, shadeIdx, seedChroma, hue, 1.0);
    }
}


/*
 * Color space interfaces
 */

vec3 getColorOklab(float rawLightness, float rawChroma, float hue) {
    vec3 lch = vec3(rawLightness, rawChroma, hue);
    vec3 oklab = lchToLab(lch);
    return oklabToLinearSrgb(oklab);
}

vec3 getColorCielab(float rawLightness, float rawChroma, float hue) {
    vec3 lch = vec3(rawLightness * 100.0, rawChroma * 170.0, hue);
    vec3 cielab = lchToLab(lch);
    return xyzToLinearSrgb(cielabToXyz(cielab));
}

vec3 clipZcamJmhToLinearSrgb2(vec3 jmh, ZcamViewingConditions cond) {
    vec3 initialResult = zcamJmhToLinearSrgb(jmh, cond);
    if (linearSrgbInGamut(initialResult)) {
        return initialResult;
    }

    float lightness = jmh.r;
    float vividness = jmh.g;
    float hue = jmh.b;

    float newVividness = vividness;
    vec3 newLinearSrgb = initialResult;
    while (!linearSrgbInGamut(newLinearSrgb) && newVividness >= 0.0) {
        newVividness -= 0.001;
        newLinearSrgb = zcamJmhToLinearSrgb(vec3(lightness, newVividness, hue), cond);
    }
    newVividness = vividness;
    while (!linearSrgbInGamut(newLinearSrgb) && newVividness <= 59.0) {
        newVividness += 0.001;
        newLinearSrgb = zcamJmhToLinearSrgb(vec3(lightness, newVividness, hue), cond);
    }

    return newLinearSrgb;
}

vec3 getColorZcam(float rawLightness, float rawChroma, float hue) {
    ZcamViewingConditions cond = createZcamViewingConditions(SURROUND_AVERAGE, 40.0, 20.0, D65 * SRGB_WHITE_LUMINANCE);

    vec3 jmh = vec3(rawLightness * 100.0, rawChroma * 80.0, hue);

    if (CLIP_ZCAM) {
        return clipZcamJmhToLinearSrgb2(jmh, cond);
    } else {
        return zcamJmhToLinearSrgb(jmh, cond);
    }
}


vec3 getLightnessOklab(float rawLightness, float rawChroma, float hue) {
    vec3 lch = vec3(rawChroma, 0.0, hue);
    vec3 oklab = lchToLab(lch);
    return oklabToLinearSrgb(oklab);
}

vec3 getLightnessCielab(float rawLightness, float rawChroma, float hue) {
    vec3 lch = vec3(rawChroma * 100.0, 0.0, hue);
    vec3 cielab = lchToLab(lch);
    return xyzToLinearSrgb(cielabToXyz(cielab));
}

vec3 getLightnessZcam(float rawLightness, float rawChroma, float hue) {
    ZcamViewingConditions cond = createZcamViewingConditions(SURROUND_AVERAGE, 40.0, 20.0, D65 * SRGB_WHITE_LUMINANCE);

    vec3 zcam = vec3(rawChroma * 100.0, 0.0, hue);

    vec3 xyzAbs = zcamToXyz(zcam, cond);
    vec3 xyzRel = xyzAbs / SRGB_WHITE_LUMINANCE;
    return xyzToLinearSrgb(xyzRel);
}
// Based on http://www.oscars.org/science-technology/sci-tech-projects/aces
vec3 aces_tonemap(vec3 color){
	mat3 m1 = mat3(
        0.59719, 0.07600, 0.02840,
        0.35458, 0.90834, 0.13383,
        0.04823, 0.01566, 0.83777
	);
	mat3 m2 = mat3(
        1.60475, -0.10208, -0.00327,
        -0.53108,  1.10813, -0.07276,
        -0.07367, -0.00605,  1.07602
	);
	vec3 v = m1 * color;
	vec3 a = v * (v + 0.0245786) - 0.000090537;
	vec3 b = v * (0.983729 * v + 0.4329510) + 0.238081;
	return pow(clamp(m2 * (a / b), 0.0, 1.0), vec3(1.0 / 2.2));
}

vec3 Tonemap_Aces(vec3 color) {

	// ACES filmic tonemapper with highlight desaturation ("crosstalk").
	// Based on the curve fit by Krzysztof Narkowicz.
	// https://knarkowicz.wordpress.com/2016/01/06/aces-filmic-tone-mapping-curve/

	const float slope = 12.0f; // higher values = slower rise.

	// Store grayscale as an extra channel.
	vec4 x = vec4(
		// RGB
		color.r, color.g, color.b,
		// Luminosity
		(color.r * 0.299) + (color.g * 0.587) + (color.b * 0.114)
	);

	// ACES Tonemapper
	const float a = 2.51f;
	const float b = 0.03f;
	const float c = 2.43f;
	const float d = 0.59f;
	const float e = 0.14f;

	vec4 tonemap = clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
	float t = x.a;

	t = t * t / (slope + t);

	// Return after desaturation step.
	return mix(tonemap.rgb, tonemap.aaa, t);
}
vec3 ACESFilm(vec3 x)
{
    float a = 2.51f;
    float b = 0.03f;
    float c = 2.43f;
    float d = 0.59f;
    float e = 0.14f;
    return clamp((x*(a*x+b))/(x*(c*x+d)+e), 0.0, 1.0);
}
/*
 * Main
 */

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy; // NDC (0-1)

    float rawLightness = uv.y;
    float rawChroma = uv.x;
    float hue = mod(iTime * HUE_RATE, 360.0); // degrees
    vec3 camOut;

    // Rainbow
    //rawLightness = 0.7502;
    //rawChroma = 0.127552;

    // Gamut/cusp animation
    camOut = getColorOklab(rawLightness, rawChroma, hue);

    // Lightness ramp
    /*if (iMouse.z > 0.0) {
        camOut = getLightnessZcam(rawLightness, rawChroma, hue);
    } else {
        camOut = getLightnessOklab(rawLightness, rawChroma, hue);
    }*/

    // Theme generation
    //camOut = getThemeColor(uv, hue);

    // Oklab gamut clipping
    //camOut = gamut_clip_preserve_lightness(camOut);
    //camOut = gamut_clip_project_to_0_5(camOut);
    //camOut = gamut_clip_project_to_L_cusp(camOut);
    //camOut = gamut_clip_adaptive_L0_0_5(camOut, 0.05);
    //camOut = gamut_clip_adaptive_L0_L_cusp(camOut, 0.05);

    // Simple RGB clipping (also necessary for Oklab clipping)
    camOut = Tonemap_Aces(camOut);
    camOut = clamp(camOut, 0.0, 1.0);

    if (linearSrgbInGamut(camOut)) {
        fragColor = vec4(srgbTransfer(camOut), 1.0);
    } else {
	    vec2 fontSize = vec2(16.0, 30.0);
        float digit = PrintValue((fragCoord - vec2(iResolution.x - 80.0, 10.0)) / fontSize, hue, 3.0, 0.0);
        if (abs(uv.x * 80.0 - 50.0 * SWATCH_CHROMA_SCALES[0]) <= 0.1 ||
                abs(uv.x * 80.0 - 50.0 * SWATCH_CHROMA_SCALES[1]) <= 0.1 ||
                abs(uv.x * 80.0 - 50.0 * SWATCH_CHROMA_SCALES[2]) <= 0.1 ||
                abs(uv.x * 80.0 - 50.0 * SWATCH_CHROMA_SCALES[3]) <= 0.1 ||
                abs(uv.x * 80.0 - 50.0 * SWATCH_CHROMA_SCALES[4]) <= 0.1) {
            digit += 1.0;
        }
        fragColor = vec4(vec3(0.5) + digit, 1.0);
    }
}
