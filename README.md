# GL Color Test

GLSL fragment shader for testing color science and palette generation algorithms interactively. **[Try on Shadertoy](https://www.shadertoy.com/view/fsd3R2)**

This project is an Android app for convenient testing on mobile devices, but the shader is compatible with Shadertoy.

See [shader_frag.frag](https://github.com/kdrag0n/glcolortest/blob/main/app/src/main/res/raw/shader_frag.frag) for the actual shader code.

## Features

- Perceptually-uniform color spaces
  - [Oklab](https://bottosson.github.io/posts/oklab/)
  - [CIELAB](https://en.wikipedia.org/wiki/CIELAB_color_space)
- Color appearance models
  - [ZCAM](https://www.osapublishing.org/oe/fulltext.cfm?uri=oe-29-4-6036&id=447640)
- Hue-preserving gamut mapping
  - Preserve lightness, reduce chroma (default)
  - Project towards neutral 50% gray
  - Adaptive lightness and chroma preservation
- [CIE 1931 XYZ](https://en.wikipedia.org/wiki/CIE_1931_color_space) interchange color space
  - Relative luminance
  - Absolute luminance in nits (cd/m²) for HDR color spaces
- Lab to LCh (lightness, chroma, hue) transformation
- [Gamut shape and cusp visualization](https://www.shadertoy.com/view/fsd3R2)
- [Material You color palette generation with time-varying hue](https://www.shadertoy.com/view/fstGz2)
- [Color gradient blending](https://www.shadertoy.com/view/NdtGz2)
- Comparing chromatic colors to neutral colors of the same lightness
- [Neutral gray lightness ramp](https://www.shadertoy.com/view/Nsd3R2)
- Rainbow hue gradient
- Dynamic CAM viewing conditions
- Device color spaces with wide color gamuts
  - Display-P3
  - DCI-P3
  - BT.2020

To enable features and render visualizations other than the default gamut plot, uncomment the respective sections in `mainImage`.

## Screenshots

### ZCAM gamut

![ZCAM gamut](https://user-images.githubusercontent.com/7930239/131246649-f0a2156f-643d-42a8-8a19-0dd53410d288.png)

### Oklab gamut

![Oklab gamut](https://user-images.githubusercontent.com/7930239/131246673-19409261-8d75-4abe-be25-4fe6a08c6e1f.png)

### ZCAM gamut clipping

![ZCAM gamut clipping](https://user-images.githubusercontent.com/7930239/131246709-88ab47bb-a29a-41d9-9a21-b7371775039a.png)

### Oklab gamut clipping

![Oklab gamut clipping](https://user-images.githubusercontent.com/7930239/131246761-59bed020-6220-4f4a-bf91-ec61faad15b5.png)

### ZCAM lightness ramp

![ZCAM lightness ramp](https://user-images.githubusercontent.com/7930239/131246787-17de9cd6-22ef-4456-baad-4483fb6ab9ba.png)

### Oklab lightness ramp

![Oklab lightness ramp](https://user-images.githubusercontent.com/7930239/131246810-42807a98-f000-4aa8-943b-6020589ad251.png)

### Material You color palette (ZCAM)

![Material You color palette (ZCAM)](https://user-images.githubusercontent.com/7930239/131246991-28f66917-30a5-448a-bfe2-83dfb654d283.png)

### Material You color palette (Oklab)

![Material You color palette (Oklab)](https://user-images.githubusercontent.com/7930239/131246993-1cd2b17d-0b5a-43b9-9708-a8468b81b7c5.png)

### Chromatic color comparison

![Chromatic color comparison](https://user-images.githubusercontent.com/7930239/131247031-d8dbbbd3-8c01-46cd-a296-2b7c748d0047.png)

### Blending red with white

ZCAM is on the top and sRGB (non-linear) is on the bottom.

![Blending red with white](https://user-images.githubusercontent.com/7930239/131247448-115df262-fdd9-4628-88a2-4f3fe864d618.png)

### Oklab rainbow

![Oklab rainbow](https://user-images.githubusercontent.com/7930239/131247087-749aadbd-00c9-4466-9aa3-404fda9fd785.png)
