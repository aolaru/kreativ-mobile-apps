import AppKit

// Deterministic Play feature graphic using the app's original brand asset.
let root = URL(fileURLWithPath: CommandLine.arguments[1])
let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 1024,
    pixelsHigh: 500, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true,
    isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
let context = NSGraphicsContext(bitmapImageRep: bitmap)!
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = context
NSColor.white.setFill()
NSBezierPath(rect: NSRect(x: 0, y: 0, width: 1024, height: 500)).fill()
NSColor(red: 0.31, green: 0.275, blue: 0.898, alpha: 1).setFill()
NSBezierPath(rect: NSRect(x: 0, y: 0, width: 1024, height: 12)).fill()
let logo = NSImage(contentsOf: root.appendingPathComponent("app/src/main/res/drawable-nodpi/kreativ_logo.png"))!
logo.draw(in: NSRect(x: 72, y: 258, width: 122, height: 122))
func text(_ value: String, _ rect: NSRect, _ size: CGFloat, _ weight: NSFont.Weight, _ color: NSColor) {
    (value as NSString).draw(in: rect, withAttributes: [
        .font: NSFont.systemFont(ofSize: size, weight: weight),
        .foregroundColor: color
    ])
}
let dark = NSColor(red: 0.071, green: 0.086, blue: 0.133, alpha: 1)
let muted = NSColor(red: 0.4, green: 0.439, blue: 0.561, alpha: 1)
text("Kreativ Font", NSRect(x: 228, y: 303, width: 720, height: 68), 54, .semibold, dark)
text("CURATED FONTS AND TOOLS", NSRect(x: 231, y: 265, width: 700, height: 32), 20, .medium, muted)
text("Commercial & free fonts", NSRect(x: 72, y: 153, width: 880, height: 60), 40, .semibold, dark)
text("Explore previews. Read articles. Save favorites.", NSRect(x: 74, y: 101, width: 876, height: 40), 25, .regular, muted)
text("kreativfont.com", NSRect(x: 74, y: 50, width: 876, height: 30), 21, .medium,
     NSColor(red: 0.31, green: 0.275, blue: 0.898, alpha: 1))
NSGraphicsContext.restoreGraphicsState()
try bitmap.representation(using: .png, properties: [:])!.write(to:
    root.appendingPathComponent("review/play-listing-2.2.1/feature-graphic.png"))
