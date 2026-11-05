import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile


spec = importlib.util.spec_from_file_location(
    "verify_release_optimization", Path(__file__).with_name("verify-release-optimization.py"))
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class ReleaseOptimizationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.mapping = self.root / "mapping.txt"
        self.mapping.write_text("com.kreativ.sharesafe.EditorSession -> a.b:\n")
        self.configuration = self.root / "configuration.txt"
        self.configuration.write_text("-keepattributes SourceFile,LineNumberTable\n")
        self.aab = self.root / "app.aab"
        self.archive(self.aab, 112)

    def archive(self, path, size, embedded_mapping=None):
        dex = bytearray(size)
        dex[:8] = b"dex\n035\0"
        struct.pack_into("<I", dex, 40, 0x12345678)
        struct.pack_into("<I", dex, 88, 3)
        struct.pack_into("<I", dex, 96, 2)
        with zipfile.ZipFile(path, "w") as bundle:
            bundle.writestr("base/dex/classes.dex", dex)
            bundle.writestr(verifier.MAPPING_ENTRY,
                            self.mapping.read_bytes() if embedded_mapping is None else embedded_mapping)

    def verify(self, baseline=None):
        return verifier.verify(self.aab, self.mapping, self.configuration, baseline)

    def test_mapping_and_embedded_metadata_match(self):
        result = self.verify()
        self.assertEqual(1, result["renamed_app_classes"])
        self.assertEqual(2, result["optimized"]["dex_class_definitions"])
        self.assertEqual(3, result["optimized"]["dex_method_references"])

    def test_rejects_mismatched_embedded_mapping(self):
        self.archive(self.aab, 112, b"wrong mapping")
        with self.assertRaisesRegex(ValueError, "Embedded Play mapping"):
            self.verify()

    def test_rejects_unobfuscated_app(self):
        self.mapping.write_text("com.kreativ.sharesafe.EditorSession -> com.kreativ.sharesafe.EditorSession:\n")
        with self.assertRaisesRegex(ValueError, "obfuscated ShareSafe classes"):
            self.verify()

    def test_rejects_disabled_optimization_flags(self):
        for flag in ("-dontoptimize", "-dontobfuscate", "-dontshrink"):
            with self.subTest(flag=flag):
                self.configuration.write_text(flag + " # inherited flag\n")
                with self.assertRaisesRegex(ValueError, "disabled"):
                    self.verify()

    def test_reports_dex_size_reduction(self):
        baseline = self.root / "baseline.aab"
        self.archive(baseline, 224)
        self.assertEqual(50.0, self.verify(baseline)["dex_size_reduction_percent"])

    def test_rejects_missing_dex(self):
        with zipfile.ZipFile(self.aab, "w") as bundle:
            bundle.writestr(verifier.MAPPING_ENTRY, self.mapping.read_bytes())
        with self.assertRaisesRegex(ValueError, "No base DEX"):
            self.verify()


if __name__ == "__main__":
    unittest.main()
