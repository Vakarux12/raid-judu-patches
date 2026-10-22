package app.raidjudu.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode

// judu is flutter, its map lives in dart, so the reports ride on a native layer above it
object MainActivityInitFingerprint : Fingerprint(
    definingClass = "Lcom/judu/MainActivity;",
    name = "<init>",
)

// dart loads the map style by this url, send it to the extension's local server instead.
// same length, the snapshot stores the size in front. Raid.java has the same url
private val styleUrlPatch = resourcePatch {
    execute {
        // the libs come from the abi split, an installed app or a base apk alone has none
        val libs = listApkEntries("lib/").filter { it.endsWith("/libapp.so") }
        if (libs.isEmpty()) throw PatchException("libapp.so not found, patch the full JUDU xapk with its arm64_v8a split, not the installed app or a base apk")
        val from = "https://maps.judu.lt/styles/bright/style.json"
        val to = "http://127.0.0.1:47123/styles/bright/style.js"
        libs.forEach { path ->
            val lib = get(path)
            val bytes = lib.readBytes()
            val at = String(bytes, Charsets.ISO_8859_1).indexOf(from)
            if (at < 0 || from.length != to.length) throw PatchException("map style url not found in $path")
            to.toByteArray(Charsets.ISO_8859_1).copyInto(bytes, at)
            lib.writeBytes(bytes)
        }
    }
}

@Suppress("unused")
val raidInspectorsPatch = bytecodePatch(
    name = "raid.lt inspectors",
    description = "Puts raid.lt ticket inspector reports on the JUDU map, with a counter on top, tap it for the reports sorted by distance.",
    default = true,
) {
    dependsOn(styleUrlPatch)

    compatibleWith(
        Compatibility(
            name = "JUDU",
            packageName = "com.judu",
            apkFileType = ApkFileType.XAPK,
            appIconColor = 0xE30613,
            targets = listOf(AppTarget(version = "1.6.0")),
        )
    )

    extendWith("extensions/extension.mpe")

    execute {
        MainActivityInitFingerprint.method.apply {
            // after super.<init>, this is unusable before it
            val end = instructions.indexOfFirst { it.opcode == Opcode.RETURN_VOID }
            addInstruction(end, "invoke-static {p0}, Lapp/raidjudu/extension/Raid;->attach(Landroid/app/Activity;)V")
        }
    }
}
