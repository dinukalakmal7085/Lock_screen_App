# Cinematic artwork

Nine original AI-generated wallpapers were created for Luma from written scene descriptions inspired by the user’s theme examples. The competitor’s screenshots, artwork, logos and UI were not extracted or bundled. Each image contains no clock; the app draws the live time separately.

The artwork is bundled offline. Six scenes have authored subject boundaries that redraw the same bitmap above the clock. This applies only to these built-in scenes and does not perform automatic segmentation on imported photos.

| Theme | Dimensions | Depth | SHA-256 |
| --- | --- | --- | --- |
| Abyss | 839 × 1874 | Yes | `fa6f380dfc2a3d9866166a68743ec6df6a741c45acd5474d7c655fadeb402af2` |
| Cloudwalker | 841 × 1870 | Yes | `075b174fac57c7338b0a0ea54b8651febabe933c327d2618e412e821bec9375b` |
| Velocity | 841 × 1870 | Yes | `27c0f1ded2e17a65ada16b75c8c46dec88e85350af850abb99d9dfbad74dba4c` |
| Stillwater | 941 × 1672 | Yes | `a6614e275d3ed4aee8299d1f3196193047abeb965eb1488cedb69b28ceb3ea1a` |
| Eclipse Guardian | 841 × 1870 | Yes | `cd5c981a4cee50c56ee4218d526cd1c4b21f713c8e880ad08966488840e8c726` |
| Emerald Halo | 841 × 1870 | No | `d3ef774924fae8a1fd1b98a2eddca1e94862cf5e600c926caa8f2417e9d36930` |
| Helio Dunes | 836 × 1881 | No | `1480dcd2216b2dce290cd8e1fbbabfc47be1369fb6501ceecf40cd504c9da86a` |
| Soft Orbit | 838 × 1877 | No | `c889331ea8186d9470e93d42b2f557a17b033cd4d79c786b664e912091985cda` |
| Stadium Flight | 841 × 1870 | Yes | `dd4a02ed5dd91ba67f0bd6311c743e04c670e7ab3657d469d0d218ba5a1f3e29` |

Previous procedural theme IDs 0–31 remain unchanged. The new cinematic themes use IDs 32–40. The gallery decodes bounded thumbnails and caches them separately from full-size artwork. Live text, crop transforms and subject clipping are covered by native rendering checks.

Glass XL uses native font outlines stretched vertically, a wallpaper sample, a translucent highlight and a fine rim. It is an original glass treatment; it does not reproduce Apple’s proprietary rendering engine.

## Clock numeral geometry

Glass XL uses original vector paths authored in `ClockNumerals.java`. They are part of the application code and do not use a copied or bundled third-party font. The glass surface is generated at runtime from the chosen wallpaper; no clock is embedded in the artwork.
