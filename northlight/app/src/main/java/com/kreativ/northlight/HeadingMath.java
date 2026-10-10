package com.kreativ.northlight;

/** Pure heading calculations, shared by the compass and saved-bearing guidance. */
final class HeadingMath {
    private HeadingMath() { }
    static float normalize(float degrees) { return (degrees % 360f + 360f) % 360f; }
    static int rounded(float degrees) { return Math.round(normalize(degrees)) % 360; }
    static float difference(float target, float current) { return normalize(target - current + 180f) - 180f; }
    // SensorManager axis constants: X=1, Y=2, minus flag=128. Display rotations are 0..3.
    static int[] axes(int rotation) {
        switch (rotation) {
            case 1: return new int[]{2, 129};
            case 2: return new int[]{129, 130};
            case 3: return new int[]{130, 1};
            default: return new int[]{1, 2};
        }
    }
}
