"""Regression checks for the model-assets release gate."""
import hashlib
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_swipe_models import (
    LFS_POINTER,
    MODEL_FILES,
    ModelVerificationError,
    verify_apk,
    verify_directory,
    verify_model,
)


class SwipeAssetVerificationTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.root = Path(self.temp_dir.name)
        self.model = MODEL_FILES[0]
        self.data = b"ET" + (b"valid model fixture " * 140)
        self.write_model(self.data)

    def write_model(self, data, metadata_override=None):
        file = self.root / self.model
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_bytes(data)
        metadata = {
            "file_sizes": {file.name: len(data)},
            "file_hashes": {file.name: hashlib.sha256(data).hexdigest()},
        }
        if metadata_override:
            metadata.update(metadata_override)
        (file.parent / "metadata.json").write_text(json.dumps(metadata))

    def test_valid_model(self):
        self.assertEqual(
            len(self.data),
            verify_model(lambda name: (self.root / name).read_bytes(), self.model),
        )

    def test_lfs_pointer_rejected_even_if_metadata_matches(self):
        self.write_model(LFS_POINTER + b"\noid sha256:deadbeef\nsize 2000000\n")
        with self.assertRaisesRegex(ModelVerificationError, "Git LFS pointer"):
            verify_model(lambda name: (self.root / name).read_bytes(), self.model)

    def test_truncated_model_rejected(self):
        self.write_model(b"ET")
        with self.assertRaisesRegex(ModelVerificationError, "truncated"):
            verify_model(lambda name: (self.root / name).read_bytes(), self.model)

    def test_hash_mismatch_rejected(self):
        self.write_model(self.data)
        (self.root / self.model).write_bytes(b"X" + self.data[1:])
        with self.assertRaisesRegex(ModelVerificationError, "SHA-256"):
            verify_model(lambda name: (self.root / name).read_bytes(), self.model)

    def test_incorrect_size_rejected(self):
        self.write_model(self.data, {"file_sizes": {Path(self.model).name: 1234}})
        with self.assertRaisesRegex(ModelVerificationError, "size"):
            verify_model(lambda name: (self.root / name).read_bytes(), self.model)

    def test_directory_and_packaged_apk(self):
        for model_path in MODEL_FILES[1:]:
            current = self.model
            self.model = model_path
            self.write_model(self.data)
            self.model = current
        vocab = self.root / "chosen_planarian/vocab.txt"
        vocab.write_text("a\nb\n")
        self.assertEqual(3, len(verify_directory(self.root)))
        apk_path = self.root / "keyboard.apk"
        with ZipFile(apk_path, "w") as apk:
            for file in self.root.rglob("*"):
                if file.is_file() and file != apk_path:
                    apk.write(file, "assets/futo-swipe/" + file.relative_to(self.root).as_posix())
        self.assertEqual(3, len(verify_apk(apk_path)))

    def test_missing_optional_model_is_build_failure(self):
        with self.assertRaisesRegex(ModelVerificationError, "missing model"):
            verify_directory(self.root)


if __name__ == "__main__":
    unittest.main()
