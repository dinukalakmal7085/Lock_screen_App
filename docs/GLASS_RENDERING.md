# Glass clock rendering: 1.3.0 beta

The supplied examples show tall, tabular numerals with a translucent body, directional beveled edges and a subject layered above the clock. Luma's renderer now separates numeral geometry, material lighting and foreground composition.

Glass XL uses ten original vector numeral outlines with fixed advances, rounded counters, consistent stroke weight and a balanced colon. Height and width are independent editor controls. Other styles and localized non-Latin text use Android font outlines. These vector numerals are original app code; no reference fonts or screenshots are bundled.

Clear and Frosted sample the wallpaper behind the clock. A distance field measures each pixel's distance to the numeral boundary, producing curved bevel normals, directional highlights and local refraction. Frosted uses softer sampling and a stronger body. Solid draws the same vector geometry with a clean fill. The material uses bounded supersampling for smoother edges, with a lower thumbnail budget. Unchanged frames reuse the cached surface; a time or geometry change regenerates it. This is a rendered material with fixed lighting, not a real-time simulation of physical glass.

These **720 × 1600** images are exported by the actual Android face renderer. The sample times are supplied only by the screenshot tests; the installed app displays live system time. The simplified face proofs omit interaction controls so the material and depth can be inspected.

| Scene and test time | Clear | Frosted | Solid |
| --- | --- | --- | --- |
| Abyss, 09:41 | [View](screenshots/glass-abyss-clear.png) | [View](screenshots/glass-abyss-frosted.png) | [View](screenshots/glass-abyss-solid.png) |
| Cloudwalker, 16:28 | [View](screenshots/glass-cloudwalker-clear.png) | [View](screenshots/glass-cloudwalker-frosted.png) | [View](screenshots/glass-cloudwalker-solid.png) |
| Eclipse Guardian, 23:57 | [View](screenshots/glass-eclipse-clear.png) | [View](screenshots/glass-eclipse-frosted.png) | [View](screenshots/glass-eclipse-solid.png) |

Automated checks cover all ten distinct digit shapes and stable advances, bevel/body contrast, retained backdrop color, transparent pixels outside the glyphs, cache reuse and live text changes. Existing tests cover theme depth, saved controls, clock width, timezone changes and the wake lifecycle. These checks support a visual beta; Redmi display quality, scrolling and editor responsiveness still need a physical-device review before production sign-off.
