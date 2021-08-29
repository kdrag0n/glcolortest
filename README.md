# GL Color Test

GLSL fragment shader for testing color science and palette generation algorithms interactively. **[Try on Shadertoy](https://www.shadertoy.com/view/fsd3R2)**

This project is an Android app for convenient testing on mobile devices, but the [actual shader](https://github.com/kdrag0n/glcolortest/blob/main/app/src/main/res/raw/shader_frag.frag) is compatible with Shadertoy.

## Features

- Perceptually-uniform color spaces
  - CIELAB
  - Oklab
- Lab to LCh (lightness, chroma, hue) transformation
- Gamut shape and cusp visualization
- Hue-preserving gamut clipping
- Material You color palette generation with time-varying hue
- Color gradient blending
- Comparing chromatic colors to neutral colors of the same lightness
- Neutral gray lightness ramp
- Rainbow hue gradient
- Dynamic CAM viewing conditions
- Device color spaces with wide gamuts: Display-P3 and BT.2020

To enable features and render visualizations other than the default gamut plot, uncomment the respective sections in `mainImage`.
