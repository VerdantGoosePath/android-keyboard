#!/usr/bin/env python3
"""Reject missing, truncated or Git LFS pointer swipe models before publishing an APK.

Model metadata in the pinned futo-swipe submodule supplies size and SHA-256.
Can validate both the checkout and the exact asset bytes in a built APK.
"""
import argparse
import hashlib
import json
from pathlib import Path
import sys
from zipfile import ZipFile

MODEL_FILES = (
    "honorable_sturgeon/model_fp32.pte",
    "magic_macaw/model_fp32.pte",
    "chosen_planarian/context_lm.pte",
)
VOCAB_FILE = "chosen_planarian/vocab.txt"
LFS_POINTER = b"version https://git-lfs.github.com/spec/v1"


class ModelVerificationError(ValueError):
    pass


def verify_model(read_bytes, model_path):
    try:
        data = read_bytes(model_path)
        metadata_path = model_path.rsplit("/", 1)[0] + "/metadata.json"
        metadata = json.loads(read_bytes(metadata_path))
    except (OSError, KeyError, ValueError) as error:
        raise ModelVerificationError(f"{model_path}: missing model or metadata: {error}") from error

    if data.startswith(LFS_POINTER):
        raise ModelVerificationError(f"{model_path}: Git LFS pointer, not a model (run git lfs pull in the submodule)")
    if len(data) < 1024:
        raise ModelVerificationError(f"{model_path}: truncated model ({len(data)} bytes)")

    basename = model_path.rsplit("/", 1)[1]
    expected_size = metadata.get("file_sizes", {}).get(basename)
    expected_hash = metadata.get("file_hashes", {}).get(basename)
    if not isinstance(expected_size, int) or not isinstance(expected_hash, str) or len(expected_hash) != 64:
        raise ModelVerificationError(f"{model_path}: missing model size or SHA-256 in metadata")
    if len(data) != expected_size:
        raise ModelVerificationError(f"{model_path}: size {len(data)} != metadata {expected_size}")
    digest = hashlib.sha256(data).hexdigest()
    if digest.lower() != expected_hash.lower():
        raise ModelVerificationError(f"{model_path}: SHA-256 differs from metadata")
    return len(data)


def verify_assets(read_bytes):
    sizes = {name: verify_model(read_bytes, name) for name in MODEL_FILES}
    try:
        vocab = read_bytes(VOCAB_FILE)
    except (OSError, KeyError) as error:
        raise ModelVerificationError(f"{VOCAB_FILE}: not included: {error}") from error
    if not vocab.strip():
        raise ModelVerificationError(f"{VOCAB_FILE}: empty vocabulary")
    return sizes


def verify_directory(root):
    return verify_assets(lambda name: (Path(root) / name).read_bytes())


def verify_apk(apk_path):
    with ZipFile(apk_path) as apk:
        return verify_assets(lambda name: apk.read("assets/futo-swipe/" + name))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--directory", type=Path, help="java/assets/futo-swipe directory")
    source.add_argument("--apk", type=Path, help="built APK to verify")
    args = parser.parse_args()
    try:
        sizes = verify_directory(args.directory) if args.directory else verify_apk(args.apk)
    except (ModelVerificationError, OSError, KeyError, ValueError) as error:
        parser.exit(1, f"Swipe asset verification failed: {error}\n")
    for path, size in sizes.items():
        print(f"Verified {path}: {size} bytes (SHA-256 matches metadata)")


if __name__ == "__main__":
    main()
