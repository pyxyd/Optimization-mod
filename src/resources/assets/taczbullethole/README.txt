This namespace exists so that the texture id

    taczbullethole:block/bullet_hole

always resolves to a namespace that a loaded pack declares.

No .png ships under this namespace on purpose. The image is staged at runtime
into a generated resource pack at

    mods/Bullet-hole-picture/assets/taczbullethole/textures/block/bullet_hole.png

which is what the block atlas actually stitches. Keeping this namespace free of
a texture avoids the mod jar and the generated pack both claiming the same
sprite.

The built-in default lives at a path the atlas never looks at:

    assets/taczopt_picture/bullet_hole.png

and is copied out to the folder the first time the game runs. See
mods/Bullet-hole-picture/README.txt for how to replace it.
